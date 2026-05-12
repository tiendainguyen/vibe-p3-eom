package com.example.eom.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Current inventory levels for a product")
public record InventoryResponse(

        @Schema(description = "Inventory record ID", example = "1")
        Long id,

        @Schema(description = "Product ID this inventory record belongs to", example = "42")
        Long productId,

        @Schema(description = "Total units physically in stock", example = "100")
        int quantityOnHand,

        @Schema(description = "Units reserved (pending payment confirmation)", example = "5")
        int quantityReserved,

        @Schema(description = "Units available for new orders (onHand − reserved)", example = "95")
        int availableQuantity,

        @Schema(description = "Last update timestamp")
        Instant updatedAt
) {}
