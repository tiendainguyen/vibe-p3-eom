package com.example.eom.dto.order;

import com.example.eom.domain.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Order details including all line items")
public record OrderResponse(

        @Schema(description = "Order ID", example = "1001")
        Long id,

        @Schema(description = "ID of the user who placed the order", example = "7")
        Long userId,

        @Schema(description = "Current order status", example = "PENDING")
        OrderStatus status,

        @Schema(description = "Line items with snapshotted prices")
        List<OrderItemResponse> items,

        @Schema(description = "Order grand total", example = "269.97")
        BigDecimal totalAmount,

        @Schema(description = "Stripe PaymentIntent ID — null until payment initiated", example = "pi_3OxK...")
        String stripePaymentIntentId,

        @Schema(description = "Order creation timestamp")
        Instant createdAt,

        @Schema(description = "Last status update timestamp")
        Instant updatedAt
) {}
