package com.example.eom.service;

import com.example.eom.domain.Order;
import com.example.eom.domain.OrderItem;
import com.example.eom.domain.enums.OrderStatus;
import com.example.eom.dto.cart.CartItemResponse;
import com.example.eom.dto.cart.CartResponse;
import com.example.eom.dto.order.OrderResponse;
import com.example.eom.dto.order.UpdateOrderStatusRequest;
import com.example.eom.repository.OrderItemRepository;
import com.example.eom.repository.OrderRepository;
import com.example.eom.service.impl.OrderServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock CartService cartService;
    @Mock InventoryService inventoryService;
    @InjectMocks OrderServiceImpl orderService;

    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 100L;

    private CartItemResponse cartItem() {
        return new CartItemResponse(10L, "Running Shoes", new BigDecimal("89.99"), 2,
                new BigDecimal("179.98"));
    }

    private CartResponse cart(boolean empty) {
        if (empty) return new CartResponse(USER_ID, List.of(), BigDecimal.ZERO);
        return new CartResponse(USER_ID, List.of(cartItem()), new BigDecimal("179.98"));
    }

    private Order order(OrderStatus status) {
        Order o = new Order();
        o.setId(ORDER_ID);
        o.setUserId(USER_ID);
        o.setStatus(status);
        o.setTotalAmount(new BigDecimal("179.98"));
        o.setCreatedAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        return o;
    }

    private OrderItem orderItem() {
        OrderItem i = new OrderItem();
        i.setId(1L);
        i.setOrderId(ORDER_ID);
        i.setProductId(10L);
        i.setProductName("Running Shoes");
        i.setUnitPrice(new BigDecimal("89.99"));
        i.setQuantity(2);
        return i;
    }

    // ── createFromCart ────────────────────────────────────────────────────────

    @Test
    void createFromCart_happyPath_reservesInventoryAndClearsCart() {
        given(cartService.getCart(USER_ID)).willReturn(cart(false));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(ORDER_ID);
            o.setCreatedAt(Instant.now());
            o.setUpdatedAt(Instant.now());
            return o;
        });
        given(orderItemRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.createFromCart(USER_ID);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo("179.98");
        assertThat(response.items()).hasSize(1);
        verify(inventoryService).reserve(10L, 2);
        verify(cartService).clearCart(USER_ID);
    }

    @Test
    void createFromCart_emptyCart_throwsIllegalArgument() {
        given(cartService.getCart(USER_ID)).willReturn(cart(true));

        assertThatThrownBy(() -> orderService.createFromCart(USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty cart");
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_ownOrder_returnsResponse() {
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order(OrderStatus.PENDING)));
        given(orderItemRepository.findByOrderId(ORDER_ID)).willReturn(List.of(orderItem()));

        OrderResponse response = orderService.getById(USER_ID, ORDER_ID);

        assertThat(response.id()).isEqualTo(ORDER_ID);
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void getById_notFound_throwsEntityNotFound() {
        given(orderRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(USER_ID, 999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getById_wrongUser_throwsAccessDenied() {
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order(OrderStatus.PENDING)));

        assertThatThrownBy(() -> orderService.getById(99L, ORDER_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Test
    void cancel_pendingOrder_releasesInventoryAndCancels() {
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order(OrderStatus.PENDING)));
        given(orderItemRepository.findByOrderId(ORDER_ID)).willReturn(List.of(orderItem()));
        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancel(USER_ID, ORDER_ID);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService).release(10L, 2);
    }

    @Test
    void cancel_paidOrder_throwsIllegalState() {
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order(OrderStatus.PAID)));

        assertThatThrownBy(() -> orderService.cancel(USER_ID, ORDER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void cancel_wrongUser_throwsAccessDenied() {
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order(OrderStatus.PENDING)));

        assertThatThrownBy(() -> orderService.cancel(99L, ORDER_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    void updateStatus_validTransition_updatesStatus() {
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order(OrderStatus.PAID)));
        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(orderItemRepository.findByOrderId(ORDER_ID)).willReturn(List.of(orderItem()));

        OrderResponse response = orderService.updateStatus(ORDER_ID,
                new UpdateOrderStatusRequest(OrderStatus.PROCESSING));

        assertThat(response.status()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void updateStatus_invalidTransition_throwsIllegalState() {
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order(OrderStatus.PENDING)));

        assertThatThrownBy(() -> orderService.updateStatus(ORDER_ID,
                new UpdateOrderStatusRequest(OrderStatus.DELIVERED)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid status transition");
    }
}
