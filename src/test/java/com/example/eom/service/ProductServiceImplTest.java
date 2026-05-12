package com.example.eom.service;

import com.example.eom.domain.Product;
import com.example.eom.dto.product.CreateProductRequest;
import com.example.eom.dto.product.ProductResponse;
import com.example.eom.dto.product.UpdateProductRequest;
import com.example.eom.repository.ProductRepository;
import com.example.eom.service.impl.ProductServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock ProductRepository productRepository;
    @InjectMocks ProductServiceImpl productService;

    private Product activeProduct() {
        Product p = new Product();
        p.setId(1L);
        p.setName("Running Shoes");
        p.setDescription("Lightweight trail shoes");
        p.setPrice(new BigDecimal("89.99"));
        p.setCategory("footwear");
        p.setActive(true);
        return p;
    }

    @Test
    void listProducts_returnsPageOfResponses() {
        PageRequest pageable = PageRequest.of(0, 20);
        given(productRepository.findAllWithFilters(null, null, null, null, pageable))
                .willReturn(new PageImpl<>(List.of(activeProduct())));

        Page<ProductResponse> result = productService.listProducts(null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Running Shoes");
    }

    @Test
    void getById_activeProduct_returnsResponse() {
        given(productRepository.findById(1L)).willReturn(Optional.of(activeProduct()));

        ProductResponse response = productService.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.active()).isTrue();
    }

    @Test
    void getById_inactiveProduct_throwsNotFound() {
        Product inactive = activeProduct();
        inactive.setActive(false);
        given(productRepository.findById(1L)).willReturn(Optional.of(inactive));

        assertThatThrownBy(() -> productService.getById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void getById_missingProduct_throwsNotFound() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void create_validRequest_savesAndReturnsResponse() {
        CreateProductRequest request = new CreateProductRequest(
                "Running Shoes", "Lightweight", new BigDecimal("89.99"), "footwear", null);
        given(productRepository.save(any(Product.class))).willAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            p.setActive(true);
            return p;
        });

        ProductResponse response = productService.create(request);

        assertThat(response.name()).isEqualTo("Running Shoes");
        assertThat(response.price()).isEqualByComparingTo("89.99");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void update_existingProduct_appliesNonNullFields() {
        given(productRepository.findById(1L)).willReturn(Optional.of(activeProduct()));
        given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

        UpdateProductRequest request = new UpdateProductRequest("Trail Shoes", null, null, null, null);
        ProductResponse response = productService.update(1L, request);

        assertThat(response.name()).isEqualTo("Trail Shoes");
        assertThat(response.price()).isEqualByComparingTo("89.99");
    }

    @Test
    void update_missingProduct_throwsNotFound() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(99L,
                new UpdateProductRequest("X", null, null, null, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_existingProduct_setsInactive() {
        Product product = activeProduct();
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

        productService.delete(1L);

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    void delete_missingProduct_throwsNotFound() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
