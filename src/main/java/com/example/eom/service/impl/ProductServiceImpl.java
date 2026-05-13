package com.example.eom.service.impl;

import com.example.eom.domain.Product;
import com.example.eom.dto.product.CreateProductRequest;
import com.example.eom.dto.product.ProductResponse;
import com.example.eom.dto.product.UpdateProductRequest;
import com.example.eom.repository.ProductRepository;
import com.example.eom.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String CACHE_NAME = "products";
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "price", "category", "createdAt", "updatedAt");

    private final ProductRepository productRepository;

    @Override
    @Cacheable(value = CACHE_NAME, key = "{#keyword, #category, #minPrice, #maxPrice, #pageable}")
    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(String keyword, String category,
                                              BigDecimal minPrice, BigDecimal maxPrice,
                                              Pageable pageable) {
        String kw  = keyword  != null ? keyword  : "";
        String cat = category != null ? category : "";
        return productRepository
                .findAllWithFilters(kw, cat, minPrice, maxPrice, sanitizeSort(pageable))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        return toResponse(product);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .category(request.category())
                .imageUrl(request.imageUrl())
                .build();
        return toResponse(productRepository.save(product));
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));

        if (request.name() != null)        product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.price() != null)       product.setPrice(request.price());
        if (request.category() != null)    product.setCategory(request.category());
        if (request.imageUrl() != null)    product.setImageUrl(request.imageUrl());

        return toResponse(productRepository.save(product));
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        product.setActive(false);
        productRepository.save(product);
    }

    private Pageable sanitizeSort(Pageable pageable) {
        boolean valid = pageable.getSort().isUnsorted() ||
                pageable.getSort().stream().allMatch(o -> SORTABLE_FIELDS.contains(o.getProperty()));
        return valid ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id"));
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(),
                p.getPrice(), p.getCategory(), p.getImageUrl(),
                p.isActive(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
