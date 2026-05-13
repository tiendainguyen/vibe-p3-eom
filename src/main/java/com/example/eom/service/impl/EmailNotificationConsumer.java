package com.example.eom.service.impl;

import com.example.eom.config.RabbitMQConfig;
import com.example.eom.dto.notification.OrderConfirmedEvent;
import com.example.eom.dto.notification.OrderShippedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationConsumer {

    private final JavaMailSender mailSender;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CONFIRMED_QUEUE)
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.userEmail());
            message.setSubject("Order Confirmed — #" + event.orderId());
            message.setText(buildConfirmedBody(event));
            mailSender.send(message);
            log.info("Confirmation email sent: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to send confirmation email: orderId={}, error={}", event.orderId(), e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_SHIPPED_QUEUE)
    public void handleOrderShipped(OrderShippedEvent event) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.userEmail());
            message.setSubject("Your Order Has Shipped — #" + event.orderId());
            message.setText(buildShippedBody(event));
            mailSender.send(message);
            log.info("Shipping email sent: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to send shipping email: orderId={}, error={}", event.orderId(), e.getMessage());
        }
    }

    private String buildConfirmedBody(OrderConfirmedEvent event) {
        return "Thank you for your order!\n\n"
                + "Order ID: " + event.orderId() + "\n"
                + "Items: " + event.itemCount() + "\n"
                + "Total: " + event.orderTotal() + " USD\n\n"
                + "We will notify you when your order ships.";
    }

    private String buildShippedBody(OrderShippedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Great news — your order has shipped!\n\n")
          .append("Order ID: ").append(event.orderId()).append("\n");
        if (event.trackingInfo() != null && !event.trackingInfo().isBlank()) {
            sb.append("Tracking: ").append(event.trackingInfo()).append("\n");
        }
        return sb.toString();
    }
}
