package com.example.eom.service;

import com.example.eom.domain.CartItem;
import com.example.eom.domain.Product;
import com.example.eom.dto.cart.AddToCartRequest;
import com.example.eom.dto.cart.CartResponse;
import com.example.eom.dto.cart.UpdateCartItemRequest;
import com.example.eom.dto.inventory.InventoryResponse;
import com.example.eom.exception.InsufficientStockException;
import com.example.eom.repository.CartItemRepository;
import com.example.eom.repository.ProductRepository;
import com.example.eom.service.impl.CartServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock CartItemRepository cartItemRepository;
    @Mock ProductRepository productRepository;
    @Mock InventoryService inventoryService;
    @InjectMocks CartServiceImpl cartService;

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 10L;

    private Product activeProduct() {
        Product p = new Product();
        p.setId(PRODUCT_ID);
        p.setName("Running Shoes");
        p.setPrice(new BigDecimal("89.99"));
        p.setActive(true);
        return p;
    }

    private CartItem cartItem(int qty) {
        CartItem item = new CartItem();
        item.setId(1L);
        item.setUserId(USER_ID);
        item.setProductId(PRODUCT_ID);
        item.setQuantity(qty);
        item.setAddedAt(Instant.now());
        return item;
    }

    private InventoryResponse inventory(int available) {
        return new InventoryResponse(1L, PRODUCT_ID, available + 5, 5, available, Instant.now());
    }

    // ── getCart ───────────────────────────────────────────────────────────────

    @Test
    void getCart_empty_returnsZeroTotal() {
        given(cartItemRepository.findByUserId(USER_ID)).willReturn(List.of());

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response.items()).isEmpty();
        assertThat(response.total()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getCart_withItems_calculatesTotal() {
        given(cartItemRepository.findByUserId(USER_ID)).willReturn(List.of(cartItem(2)));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(activeProduct()));

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo("179.98");
        assertThat(response.total()).isEqualByComparingTo("179.98");
    }

    // ── addItem ───────────────────────────────────────────────────────────────

    @Test
    void addItem_newProduct_createsCartItem() {
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(activeProduct()));
        given(inventoryService.getByProduct(PRODUCT_ID)).willReturn(inventory(10));
        given(cartItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).willReturn(Optional.empty());
        given(cartItemRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(cartItemRepository.findByUserId(USER_ID)).willReturn(List.of(cartItem(2)));

        CartResponse response = cartService.addItem(USER_ID, new AddToCartRequest(PRODUCT_ID, 2));

        assertThat(response.items()).hasSize(1);
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addItem_existingProduct_incrementsQuantity() {
        CartItem existing = cartItem(3);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(activeProduct()));
        given(inventoryService.getByProduct(PRODUCT_ID)).willReturn(inventory(10));
        given(cartItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).willReturn(Optional.of(existing));
        given(cartItemRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(cartItemRepository.findByUserId(USER_ID)).willReturn(List.of(existing));

        cartService.addItem(USER_ID, new AddToCartRequest(PRODUCT_ID, 2));

        assertThat(existing.getQuantity()).isEqualTo(5);
    }

    @Test
    void addItem_inactiveProduct_throwsNotFound() {
        Product inactive = activeProduct();
        inactive.setActive(false);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(inactive));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddToCartRequest(PRODUCT_ID, 1)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void addItem_insufficientStock_throwsConflict() {
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(activeProduct()));
        given(inventoryService.getByProduct(PRODUCT_ID)).willReturn(inventory(1));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddToCartRequest(PRODUCT_ID, 5)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("available 1");
    }

    // ── updateItem ────────────────────────────────────────────────────────────

    @Test
    void updateItem_positiveQty_updatesQuantity() {
        CartItem item = cartItem(3);
        given(cartItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).willReturn(Optional.of(item));
        given(cartItemRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(cartItemRepository.findByUserId(USER_ID)).willReturn(List.of(item));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(activeProduct()));

        cartService.updateItem(USER_ID, PRODUCT_ID, new UpdateCartItemRequest(5));

        assertThat(item.getQuantity()).isEqualTo(5);
    }

    @Test
    void updateItem_zeroQty_removesItem() {
        given(cartItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).willReturn(Optional.of(cartItem(3)));
        given(cartItemRepository.findByUserId(USER_ID)).willReturn(List.of());

        CartResponse response = cartService.updateItem(USER_ID, PRODUCT_ID, new UpdateCartItemRequest(0));

        verify(cartItemRepository).delete(any(CartItem.class));
        assertThat(response.items()).isEmpty();
    }

    @Test
    void updateItem_itemNotInCart_throwsNotFound() {
        given(cartItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItem(USER_ID, PRODUCT_ID, new UpdateCartItemRequest(2)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── removeItem ────────────────────────────────────────────────────────────

    @Test
    void removeItem_existingItem_removesAndReturnsCart() {
        given(cartItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).willReturn(Optional.of(cartItem(2)));
        given(cartItemRepository.findByUserId(USER_ID)).willReturn(List.of());

        CartResponse response = cartService.removeItem(USER_ID, PRODUCT_ID);

        verify(cartItemRepository).delete(any(CartItem.class));
        assertThat(response.items()).isEmpty();
    }

    @Test
    void removeItem_notInCart_throwsNotFound() {
        given(cartItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(USER_ID, PRODUCT_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── clearCart ─────────────────────────────────────────────────────────────

    @Test
    void clearCart_deletesAllUserItems() {
        cartService.clearCart(USER_ID);
        verify(cartItemRepository).deleteByUserId(USER_ID);
    }
}
