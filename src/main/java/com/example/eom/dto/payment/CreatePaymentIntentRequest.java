package com.example.eom.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to create a Stripe PaymentIntent for an order")
public record CreatePaymentIntentRequest(
        @NotNull(message = "orderId is required")
        @Schema(description = "ID of the PENDING order to pay for", example = "100")
        Long orderId
) {}
