package com.example.eom.dto.cart;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Current state of the authenticated user's cart")
public record CartResponse(

        @Schema(description = "Owner user ID", example = "7")
        Long userId,

        @Schema(description = "Line items in the cart")
        List<CartItemResponse> items,

        @Schema(description = "Cart grand total", example = "269.97")
        BigDecimal total
) {}
