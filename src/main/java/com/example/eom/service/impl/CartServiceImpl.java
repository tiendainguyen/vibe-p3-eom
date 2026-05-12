package com.example.eom.service.impl;

import com.example.eom.domain.CartItem;
import com.example.eom.domain.Product;
import com.example.eom.dto.cart.AddToCartRequest;
import com.example.eom.dto.cart.CartItemResponse;
import com.example.eom.dto.cart.CartResponse;
import com.example.eom.dto.cart.UpdateCartItemRequest;
import com.example.eom.dto.inventory.InventoryResponse;
import com.example.eom.exception.InsufficientStockException;
import com.example.eom.repository.CartItemRepository;
import com.example.eom.repository.ProductRepository;
import com.example.eom.service.CartService;
import com.example.eom.service.InventoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        return buildCartResponse(userId, items);
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, AddToCartRequest request) {
        Product product = productRepository.findById(request.productId())
                .filter(Product::isActive)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.productId()));

        InventoryResponse inventory = inventoryService.getByProduct(request.productId());
        if (inventory.availableQuantity() < request.quantity()) {
            throw new InsufficientStockException(
                    request.productId(), request.quantity(), inventory.availableQuantity());
        }

        CartItem item = cartItemRepository
                .findByUserIdAndProductId(userId, request.productId())
                .orElseGet(() -> CartItem.builder()
                        .userId(userId)
                        .productId(request.productId())
                        .quantity(0)
                        .build());

        item.setQuantity(item.getQuantity() + request.quantity());
        cartItemRepository.save(item);

        return buildCartResponse(userId, cartItemRepository.findByUserId(userId));
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long userId, Long productId, UpdateCartItemRequest request) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new EntityNotFoundException("Item not in cart: product " + productId));

        if (request.quantity() == 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(request.quantity());
            cartItemRepository.save(item);
        }

        return buildCartResponse(userId, cartItemRepository.findByUserId(userId));
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new EntityNotFoundException("Item not in cart: product " + productId));
        cartItemRepository.delete(item);
        return buildCartResponse(userId, cartItemRepository.findByUserId(userId));
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    private CartResponse buildCartResponse(Long userId, List<CartItem> items) {
        List<CartItemResponse> lineItems = items.stream()
                .map(item -> {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Product not found: " + item.getProductId()));
                    BigDecimal subtotal = product.getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));
                    return new CartItemResponse(
                            item.getProductId(), product.getName(),
                            product.getPrice(), item.getQuantity(), subtotal);
                })
                .toList();

        BigDecimal total = lineItems.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(userId, lineItems, total);
    }
}
