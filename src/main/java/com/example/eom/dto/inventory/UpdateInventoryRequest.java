package com.example.eom.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body to set a product's stock quantity")
public record UpdateInventoryRequest(

        @Schema(description = "Total units physically in stock — must be ≥ current reserved quantity", example = "150")
        @NotNull(message = "quantityOnHand is required")
        @Min(value = 0, message = "quantityOnHand must be 0 or greater")
        Integer quantityOnHand
) {}
