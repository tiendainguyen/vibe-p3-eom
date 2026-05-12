package com.example.eom.service.gateway;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;

public interface StripeGateway {

    PaymentIntent createPaymentIntent(long amountCents, String currency, String orderId)
            throws StripeException;

    Event constructEvent(String payload, String sigHeader, String endpointSecret)
            throws StripeException;
}
