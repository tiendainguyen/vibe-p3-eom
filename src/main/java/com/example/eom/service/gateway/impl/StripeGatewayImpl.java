package com.example.eom.service.gateway.impl;

import com.example.eom.service.gateway.StripeGateway;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StripeGatewayImpl implements StripeGateway {

    public StripeGatewayImpl(@Value("${stripe.secret-key}") String secretKey) {
        Stripe.apiKey = secretKey;
    }

    @Override
    public PaymentIntent createPaymentIntent(long amountCents, String currency, String orderId)
            throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency)
                .putMetadata("order_id", orderId)
                .build();
        return PaymentIntent.create(params);
    }

    @Override
    public Event constructEvent(String payload, String sigHeader, String endpointSecret)
            throws StripeException {
        return Webhook.constructEvent(payload, sigHeader, endpointSecret);
    }
}
