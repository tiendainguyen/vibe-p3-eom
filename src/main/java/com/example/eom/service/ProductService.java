package com.example.eom.service;

import com.example.eom.dto.product.CreateProductRequest;
import com.example.eom.dto.product.ProductResponse;
import com.example.eom.dto.product.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {

    Page<ProductResponse> listProducts(String keyword, String category,
                                       BigDecimal minPrice, BigDecimal maxPrice,
                                       Pageable pageable);

    ProductResponse getById(Long id);

    ProductResponse create(CreateProductRequest request);

    ProductResponse update(Long id, UpdateProductRequest request);

    void delete(Long id);
}
