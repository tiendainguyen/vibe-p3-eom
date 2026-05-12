package com.example.eom.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Product details")
public record ProductResponse(

        @Schema(description = "Product ID", example = "1")
        Long id,

        @Schema(description = "Product name", example = "Running Shoes")
        String name,

        @Schema(description = "Product description", example = "Lightweight trail running shoes")
        String description,

        @Schema(description = "Price in USD", example = "89.99")
        BigDecimal price,

        @Schema(description = "Product category", example = "footwear")
        String category,

        @Schema(description = "Product image URL", example = "https://cdn.example.com/shoes.jpg")
        String imageUrl,

        @Schema(description = "Whether the product is available for purchase", example = "true")
        boolean active,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt
) {}
