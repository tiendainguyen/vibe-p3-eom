package com.example.eom.service.impl;

import com.example.eom.config.RabbitMQConfig;
import com.example.eom.domain.Order;
import com.example.eom.domain.User;
import com.example.eom.dto.notification.OrderConfirmedEvent;
import com.example.eom.dto.notification.OrderShippedEvent;
import com.example.eom.repository.OrderItemRepository;
import com.example.eom.repository.OrderRepository;
import com.example.eom.repository.UserRepository;
import com.example.eom.service.NotificationPublisher;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisherImpl implements NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Override
    public void publishOrderConfirmed(Long orderId) {
        Order order = loadOrder(orderId);
        User user = loadUser(order.getUserId());
        int itemCount = orderItemRepository.findByOrderId(orderId).size();

        OrderConfirmedEvent event = new OrderConfirmedEvent(
                orderId,
                order.getUserId(),
                user.getEmail(),
                order.getTotalAmount(),
                itemCount);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ORDER_CONFIRMED_KEY, event);
        log.info("Published OrderConfirmedEvent: orderId={}", orderId);
    }

    @Override
    public void publishOrderShipped(Long orderId, String trackingInfo) {
        Order order = loadOrder(orderId);
        User user = loadUser(order.getUserId());

        OrderShippedEvent event = new OrderShippedEvent(
                orderId,
                order.getUserId(),
                user.getEmail(),
                trackingInfo);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ORDER_SHIPPED_KEY, event);
        log.info("Published OrderShippedEvent: orderId={}", orderId);
    }

    private Order loadOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }
}
