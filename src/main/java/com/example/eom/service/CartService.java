package com.example.eom.service;

import com.example.eom.dto.cart.AddToCartRequest;
import com.example.eom.dto.cart.CartResponse;
import com.example.eom.dto.cart.UpdateCartItemRequest;

public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse addItem(Long userId, AddToCartRequest request);

    CartResponse updateItem(Long userId, Long productId, UpdateCartItemRequest request);

    CartResponse removeItem(Long userId, Long productId);

    void clearCart(Long userId);
}
