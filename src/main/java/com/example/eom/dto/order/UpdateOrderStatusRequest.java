package com.example.eom.dto.order;

import com.example.eom.domain.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to advance an order's status (admin only)")
public record UpdateOrderStatusRequest(

        @Schema(description = "New status — valid admin transitions: PAID→PROCESSING→SHIPPED→DELIVERED",
                example = "PROCESSING")
        @NotNull(message = "status is required")
        OrderStatus status,

        @Schema(description = "Tracking number or URL (optional, relevant when status is SHIPPED)",
                example = "1Z999AA10123456784")
        String trackingInfo
) {}
