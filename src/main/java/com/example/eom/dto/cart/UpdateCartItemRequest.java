package com.example.eom.dto.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update the quantity of a cart item — set to 0 to remove")
public record UpdateCartItemRequest(

        @Schema(description = "New quantity — 0 removes the item from the cart", example = "3")
        @NotNull(message = "quantity is required")
        @Min(value = 0, message = "quantity must be 0 or greater")
        Integer quantity
) {}
