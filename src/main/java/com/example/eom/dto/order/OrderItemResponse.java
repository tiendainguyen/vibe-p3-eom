package com.example.eom.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "A single line item within an order")
public record OrderItemResponse(

        @Schema(description = "Product ID", example = "1")
        Long productId,

        @Schema(description = "Product name at time of purchase (snapshotted)", example = "Running Shoes")
        String productName,

        @Schema(description = "Unit price at time of purchase (snapshotted)", example = "89.99")
        BigDecimal unitPrice,

        @Schema(description = "Quantity ordered", example = "2")
        int quantity,

        @Schema(description = "Line total (unitPrice × quantity)", example = "179.98")
        BigDecimal subtotal
) {}
