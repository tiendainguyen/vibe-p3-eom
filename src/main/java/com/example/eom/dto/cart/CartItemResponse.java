package com.example.eom.dto.cart;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "A single line item in the cart")
public record CartItemResponse(

        @Schema(description = "Product ID", example = "1")
        Long productId,

        @Schema(description = "Product name at time of cart view", example = "Running Shoes")
        String productName,

        @Schema(description = "Unit price", example = "89.99")
        BigDecimal price,

        @Schema(description = "Quantity in cart", example = "2")
        int quantity,

        @Schema(description = "Line total (price × quantity)", example = "179.98")
        BigDecimal subtotal
) {}
