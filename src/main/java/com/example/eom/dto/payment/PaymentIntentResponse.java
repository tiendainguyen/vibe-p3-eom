package com.example.eom.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Stripe PaymentIntent details returned to the client")
public record PaymentIntentResponse(
        @Schema(description = "Stripe PaymentIntent ID", example = "pi_3NxKd...")
        String paymentIntentId,

        @Schema(description = "Client secret for completing payment on the frontend", example = "pi_3NxKd..._secret_...")
        String clientSecret,

        @Schema(description = "Order ID this payment intent is linked to", example = "100")
        Long orderId,

        @Schema(description = "Amount in the currency's smallest unit (e.g. cents)", example = "17998")
        long amount,

        @Schema(description = "Three-letter ISO currency code", example = "usd")
        String currency
) {}
