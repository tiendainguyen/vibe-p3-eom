package com.example.eom.service;

import com.example.eom.domain.enums.OrderStatus;
import com.example.eom.dto.order.OrderResponse;
import com.example.eom.dto.order.UpdateOrderStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface OrderService {

    OrderResponse createFromCart(Long userId);

    OrderResponse getById(Long userId, Long orderId);

    Page<OrderResponse> listByUser(Long userId, Pageable pageable);

    OrderResponse cancel(Long userId, Long orderId);

    Page<OrderResponse> adminList(OrderStatus status, Long userId, Instant from, Instant to, Pageable pageable);

    OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request);

    void linkPaymentIntent(Long orderId, String paymentIntentId);

    void markAsPaid(String paymentIntentId);

    void markAsRefunded(String paymentIntentId);

    Long findOrderIdByPaymentIntentId(String paymentIntentId);
}
