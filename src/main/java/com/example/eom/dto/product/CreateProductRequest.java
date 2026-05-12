package com.example.eom.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Request body for creating a product")
public record CreateProductRequest(

        @Schema(description = "Product name", example = "Running Shoes")
        @NotBlank(message = "Name is required")
        String name,

        @Schema(description = "Product description", example = "Lightweight trail running shoes")
        String description,

        @Schema(description = "Price in USD — must be greater than 0", example = "89.99")
        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than 0")
        BigDecimal price,

        @Schema(description = "Product category", example = "footwear")
        String category,

        @Schema(description = "Product image URL", example = "https://cdn.example.com/shoes.jpg")
        String imageUrl
) {}
