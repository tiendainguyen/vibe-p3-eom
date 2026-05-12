package com.example.eom.controller;

import com.example.eom.dto.ErrorResponseDTO;
import com.example.eom.dto.product.ProductResponse;
import com.example.eom.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Browse and search the product catalog (public)")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "List products with optional keyword search and filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of matching products")
    })
    public Page<ProductResponse> listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return productService.listProducts(keyword, category, minPrice, maxPrice, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single active product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found or inactive",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }
}
