package com.example.eom.dto.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to add a product to the cart")
public record AddToCartRequest(

        @Schema(description = "Product ID to add", example = "1")
        @NotNull(message = "productId is required")
        Long productId,

        @Schema(description = "Number of units to add — must be at least 1", example = "2")
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity
) {}
