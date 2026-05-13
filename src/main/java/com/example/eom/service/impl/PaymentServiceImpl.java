package com.example.eom.service.impl;

import com.example.eom.dto.payment.CreatePaymentIntentRequest;
import com.example.eom.dto.payment.PaymentIntentResponse;
import com.example.eom.dto.order.OrderResponse;
import com.example.eom.service.NotificationPublisher;
import com.example.eom.service.OrderService;
import com.example.eom.service.PaymentService;
import com.example.eom.service.WebhookService;
import com.example.eom.service.gateway.StripeGateway;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final StripeGateway stripeGateway;
    private final OrderService orderService;
    private final NotificationPublisher notificationPublisher;
    private final WebhookService webhookService;
    private final String webhookSecret;

    public PaymentServiceImpl(StripeGateway stripeGateway, OrderService orderService,
                              NotificationPublisher notificationPublisher,
                              WebhookService webhookService,
                              @Value("${stripe.webhook-secret}") String webhookSecret) {
        this.stripeGateway = stripeGateway;
        this.orderService = orderService;
        this.notificationPublisher = notificationPublisher;
        this.webhookService = webhookService;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public PaymentIntentResponse createIntent(Long userId, CreatePaymentIntentRequest request) {
        OrderResponse order = orderService.getById(userId, request.orderId());

        long amountCents = order.totalAmount()
                .movePointRight(2)
                .longValue();

        PaymentIntent intent;
        try {
            intent = stripeGateway.createPaymentIntent(amountCents, "usd",
                    String.valueOf(order.id()));
        } catch (StripeException e) {
            throw new IllegalStateException("Failed to create payment intent: " + e.getMessage(), e);
        }

        orderService.linkPaymentIntent(order.id(), intent.getId());

        return new PaymentIntentResponse(
                intent.getId(),
                intent.getClientSecret(),
                order.id(),
                amountCents,
                "usd");
    }

    @Override
    public void handleWebhookEvent(String payload, String sigHeader) {
        Event event;
        try {
            event = stripeGateway.constructEvent(payload, sigHeader, webhookSecret);
        } catch (StripeException e) {
            log.warn("Webhook signature verification failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                String piId = extractPaymentIntentId(event);
                if (piId != null) {
                    orderService.markAsPaid(piId);
                    Long orderId = orderService.findOrderIdByPaymentIntentId(piId);
                    if (orderId != null) {
                        notificationPublisher.publishOrderConfirmed(orderId);
                        webhookService.dispatch(WebhookService.EVENT_ORDER_PAID, orderId);
                    }
                }
            }
            case "payment_intent.payment_failed" -> {
                String piId = extractPaymentIntentId(event);
                if (piId != null) {
                    log.warn("Payment failed for PaymentIntent: {}", piId);
                }
            }
            case "charge.refunded" -> {
                String piId = extractPaymentIntentIdFromCharge(event);
                if (piId != null) {
                    orderService.markAsRefunded(piId);
                }
            }
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private String extractPaymentIntentId(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            PaymentIntent pi = (PaymentIntent) deserializer.getObject().get();
            return pi.getId();
        }
        return null;
    }

    private String extractPaymentIntentIdFromCharge(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            com.stripe.model.Charge charge = (com.stripe.model.Charge) deserializer.getObject().get();
            return charge.getPaymentIntent();
        }
        return null;
    }
}
