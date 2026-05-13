package com.example.eom.service;

import com.example.eom.config.RabbitMQConfig;
import com.example.eom.domain.Order;
import com.example.eom.domain.OrderItem;
import com.example.eom.domain.User;
import com.example.eom.domain.enums.OrderStatus;
import com.example.eom.dto.notification.OrderConfirmedEvent;
import com.example.eom.dto.notification.OrderShippedEvent;
import com.example.eom.repository.OrderItemRepository;
import com.example.eom.repository.OrderRepository;
import com.example.eom.repository.UserRepository;
import com.example.eom.service.impl.NotificationPublisherImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationPublisherImplTest {

    @Mock RabbitTemplate rabbitTemplate;
    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock UserRepository userRepository;

    private NotificationPublisherImpl publisher;

    private static final Long ORDER_ID = 100L;
    private static final Long USER_ID  = 1L;

    @BeforeEach
    void setUp() {
        publisher = new NotificationPublisherImpl(rabbitTemplate, orderRepository,
                orderItemRepository, userRepository);
    }

    private Order sampleOrder() {
        Order o = new Order();
        o.setId(ORDER_ID);
        o.setUserId(USER_ID);
        o.setStatus(OrderStatus.PAID);
        o.setTotalAmount(new BigDecimal("59.99"));
        return o;
    }

    private User sampleUser() {
        User u = new User();
        u.setId(USER_ID);
        u.setEmail("buyer@example.com");
        return u;
    }

    // ── publishOrderConfirmed ─────────────────────────────────────────────────

    @Test
    void publishOrderConfirmed_sendsCorrectEvent() {
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(sampleOrder()));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sampleUser()));
        given(orderItemRepository.findByOrderId(ORDER_ID)).willReturn(List.of(
                new OrderItem(), new OrderItem()));

        publisher.publishOrderConfirmed(ORDER_ID);

        ArgumentCaptor<OrderConfirmedEvent> captor = ArgumentCaptor.forClass(OrderConfirmedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ORDER_CONFIRMED_KEY), captor.capture());

        OrderConfirmedEvent event = captor.getValue();
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.userEmail()).isEqualTo("buyer@example.com");
        assertThat(event.orderTotal()).isEqualByComparingTo("59.99");
        assertThat(event.itemCount()).isEqualTo(2);
    }

    // ── publishOrderShipped ───────────────────────────────────────────────────

    @Test
    void publishOrderShipped_sendsCorrectEvent() {
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(sampleOrder()));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sampleUser()));

        publisher.publishOrderShipped(ORDER_ID, "TRACK123");

        ArgumentCaptor<OrderShippedEvent> captor = ArgumentCaptor.forClass(OrderShippedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ORDER_SHIPPED_KEY), captor.capture());

        OrderShippedEvent event = captor.getValue();
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.userEmail()).isEqualTo("buyer@example.com");
        assertThat(event.trackingInfo()).isEqualTo("TRACK123");
    }

    @Test
    void publishOrderShipped_nullTrackingInfo_sendsEventWithNullTracking() {
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(sampleOrder()));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(sampleUser()));

        publisher.publishOrderShipped(ORDER_ID, null);

        ArgumentCaptor<OrderShippedEvent> captor = ArgumentCaptor.forClass(OrderShippedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ORDER_SHIPPED_KEY), captor.capture());

        assertThat(captor.getValue().trackingInfo()).isNull();
    }
}
