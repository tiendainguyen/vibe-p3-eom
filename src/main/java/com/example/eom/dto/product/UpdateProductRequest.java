package com.example.eom.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Request body for updating a product — all fields optional, null fields are ignored")
public record UpdateProductRequest(

        @Schema(description = "New product name", example = "Trail Running Shoes")
        String name,

        @Schema(description = "New description", example = "Updated lightweight trail shoes")
        String description,

        @Schema(description = "New price — must be greater than 0 if provided", example = "94.99")
        @Positive(message = "Price must be greater than 0")
        BigDecimal price,

        @Schema(description = "New category", example = "footwear")
        String category,

        @Schema(description = "New image URL", example = "https://cdn.example.com/shoes-v2.jpg")
        String imageUrl
) {}
