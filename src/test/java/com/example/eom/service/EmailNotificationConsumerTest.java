package com.example.eom.service;

import com.example.eom.dto.notification.OrderConfirmedEvent;
import com.example.eom.dto.notification.OrderShippedEvent;
import com.example.eom.service.impl.EmailNotificationConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationConsumerTest {

    @Mock JavaMailSender mailSender;

    private EmailNotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new EmailNotificationConsumer(mailSender);
    }

    // ── handleOrderConfirmed ──────────────────────────────────────────────────

    @Test
    void handleOrderConfirmed_sendsEmailToCustomer() {
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                100L, 1L, "buyer@example.com", new BigDecimal("59.99"), 3);

        consumer.handleOrderConfirmed(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getTo()).containsExactly("buyer@example.com");
        assertThat(msg.getSubject()).contains("100");
        assertThat(msg.getText()).contains("59.99");
        assertThat(msg.getText()).contains("3");
    }

    // ── handleOrderShipped ────────────────────────────────────────────────────

    @Test
    void handleOrderShipped_withTracking_includesTrackingInEmail() {
        OrderShippedEvent event = new OrderShippedEvent(
                100L, 1L, "buyer@example.com", "TRACK123");

        consumer.handleOrderShipped(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getTo()).containsExactly("buyer@example.com");
        assertThat(msg.getSubject()).contains("100");
        assertThat(msg.getText()).contains("TRACK123");
    }

    @Test
    void handleOrderShipped_withoutTracking_noTrackingLineInEmail() {
        OrderShippedEvent event = new OrderShippedEvent(
                100L, 1L, "buyer@example.com", null);

        consumer.handleOrderShipped(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getText()).doesNotContain("Tracking:");
    }

    @Test
    void handleOrderConfirmed_mailFailure_doesNotPropagate() {
        org.mockito.BDDMockito.willThrow(new RuntimeException("SMTP error"))
                .given(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        OrderConfirmedEvent event = new OrderConfirmedEvent(
                100L, 1L, "buyer@example.com", new BigDecimal("10.00"), 1);

        // must not throw
        consumer.handleOrderConfirmed(event);
    }
}
