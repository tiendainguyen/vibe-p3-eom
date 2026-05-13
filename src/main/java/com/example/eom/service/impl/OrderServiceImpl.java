package com.example.eom.service.impl;

import com.example.eom.domain.Order;
import com.example.eom.domain.OrderItem;
import com.example.eom.domain.enums.OrderStatus;
import com.example.eom.dto.cart.CartItemResponse;
import com.example.eom.dto.cart.CartResponse;
import com.example.eom.dto.order.OrderItemResponse;
import com.example.eom.dto.order.OrderResponse;
import com.example.eom.dto.order.UpdateOrderStatusRequest;
import com.example.eom.repository.OrderItemRepository;
import com.example.eom.repository.OrderRepository;
import com.example.eom.service.CartService;
import com.example.eom.service.InventoryService;
import com.example.eom.service.NotificationPublisher;
import com.example.eom.service.OrderService;
import com.example.eom.service.WebhookService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "status", "totalAmount", "createdAt", "updatedAt");

    private static final Map<OrderStatus, OrderStatus> VALID_ADMIN_TRANSITIONS;

    static {
        VALID_ADMIN_TRANSITIONS = new EnumMap<>(OrderStatus.class);
        VALID_ADMIN_TRANSITIONS.put(OrderStatus.PAID, OrderStatus.PROCESSING);
        VALID_ADMIN_TRANSITIONS.put(OrderStatus.PROCESSING, OrderStatus.SHIPPED);
        VALID_ADMIN_TRANSITIONS.put(OrderStatus.SHIPPED, OrderStatus.DELIVERED);
    }

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final InventoryService inventoryService;
    private final NotificationPublisher notificationPublisher;
    private final WebhookService webhookService;

    @Override
    @Transactional
    public OrderResponse createFromCart(Long userId) {
        CartResponse cart = cartService.getCart(userId);

        if (cart.items().isEmpty()) {
            throw new IllegalArgumentException("Cannot create order from an empty cart");
        }

        // Reserve inventory for all items — @Transactional rolls back if any reservation fails
        for (CartItemResponse item : cart.items()) {
            inventoryService.reserve(item.productId(), item.quantity());
        }

        BigDecimal total = cart.total();

        Order order = orderRepository.save(Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .build());

        List<OrderItem> orderItems = cart.items().stream()
                .map(item -> OrderItem.builder()
                        .orderId(order.getId())
                        .productId(item.productId())
                        .productName(item.productName())
                        .unitPrice(item.price())
                        .quantity(item.quantity())
                        .build())
                .toList();
        orderItemRepository.saveAll(orderItems);

        cartService.clearCart(userId);

        return toResponse(order, orderItems);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long userId, Long orderId) {
        Order order = findOrThrow(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("Order does not belong to the current user");
        }
        return toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> listByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, sanitizeSort(pageable))
                .map(order -> toResponse(order, orderItemRepository.findByOrderId(order.getId())));
    }

    @Override
    @Transactional
    public OrderResponse cancel(Long userId, Long orderId) {
        Order order = findOrThrow(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("Order does not belong to the current user");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be cancelled");
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        items.forEach(item -> inventoryService.release(item.getProductId(), item.getQuantity()));

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        return toResponse(order, items);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> adminList(OrderStatus status, Long userId, Instant from, Instant to,
                                         Pageable pageable) {
        return orderRepository.findAllWithFilters(status, userId, from, to, sanitizeSort(pageable))
                .map(order -> toResponse(order, orderItemRepository.findByOrderId(order.getId())));
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = findOrThrow(orderId);
        OrderStatus next = request.status();
        OrderStatus expected = VALID_ADMIN_TRANSITIONS.get(order.getStatus());

        if (expected == null || expected != next) {
            throw new IllegalStateException(
                    "Invalid status transition: " + order.getStatus() + " → " + next);
        }

        order.setStatus(next);
        orderRepository.save(order);

        if (next == OrderStatus.SHIPPED) {
            notificationPublisher.publishOrderShipped(orderId, request.trackingInfo());
            webhookService.dispatch(WebhookService.EVENT_ORDER_SHIPPED, orderId);
        } else if (next == OrderStatus.DELIVERED) {
            webhookService.dispatch(WebhookService.EVENT_ORDER_DELIVERED, orderId);
        }

        return toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    @Override
    @Transactional
    public void linkPaymentIntent(Long orderId, String paymentIntentId) {
        Order order = findOrThrow(orderId);
        order.setStripePaymentIntentId(paymentIntentId);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void markAsPaid(String paymentIntentId) {
        orderRepository.findByStripePaymentIntentId(paymentIntentId).ifPresent(order -> {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
        });
    }

    @Override
    @Transactional
    public void markAsRefunded(String paymentIntentId) {
        orderRepository.findByStripePaymentIntentId(paymentIntentId).ifPresent(order -> {
            order.setStatus(OrderStatus.REFUNDED);
            orderRepository.save(order);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Long findOrderIdByPaymentIntentId(String paymentIntentId) {
        return orderRepository.findByStripePaymentIntentId(paymentIntentId)
                .map(Order::getId)
                .orElse(null);
    }

    private Pageable sanitizeSort(Pageable pageable) {
        boolean valid = pageable.getSort().isUnsorted() ||
                pageable.getSort().stream().allMatch(o -> SORTABLE_FIELDS.contains(o.getProperty()));
        return valid ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id"));
    }

    private Order findOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
    }

    private OrderResponse toResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(), item.getProductName(), item.getUnitPrice(),
                        item.getQuantity(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))))
                .toList();
        return new OrderResponse(
                order.getId(), order.getUserId(), order.getStatus(),
                itemResponses, order.getTotalAmount(), order.getStripePaymentIntentId(),
                order.getCreatedAt(), order.getUpdatedAt());
    }
}
