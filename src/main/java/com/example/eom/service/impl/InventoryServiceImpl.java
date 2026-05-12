package com.example.eom.service.impl;

import com.example.eom.domain.Inventory;
import com.example.eom.dto.inventory.InventoryResponse;
import com.example.eom.dto.inventory.UpdateInventoryRequest;
import com.example.eom.exception.InsufficientStockException;
import com.example.eom.repository.InventoryRepository;
import com.example.eom.service.InventoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> list(Long productId) {
        if (productId != null) {
            return inventoryRepository.findByProductId(productId)
                    .map(this::toResponse)
                    .map(List::of)
                    .orElse(List.of());
        }
        return inventoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getByProduct(Long productId) {
        return toResponse(findOrThrow(productId));
    }

    @Override
    @Transactional
    public InventoryResponse updateStock(Long productId, UpdateInventoryRequest request) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseGet(() -> {
                    Inventory fresh = Inventory.builder()
                            .productId(productId)
                            .updatedAt(Instant.now())
                            .build();
                    return inventoryRepository.save(fresh);
                });

        if (request.quantityOnHand() < inventory.getQuantityReserved()) {
            throw new IllegalArgumentException(
                    "Cannot set quantityOnHand (" + request.quantityOnHand() +
                    ") below current reserved (" + inventory.getQuantityReserved() + ")");
        }

        inventory.setQuantityOnHand(request.quantityOnHand());
        return toResponse(inventoryRepository.save(inventory));
    }

    @Override
    @Transactional
    public void reserve(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found for product: " + productId));

        if (inventory.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(productId, quantity, inventory.getAvailableQuantity());
        }

        inventory.setQuantityReserved(inventory.getQuantityReserved() + quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void deduct(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found for product: " + productId));

        inventory.setQuantityOnHand(inventory.getQuantityOnHand() - quantity);
        inventory.setQuantityReserved(inventory.getQuantityReserved() - quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void release(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found for product: " + productId));

        inventory.setQuantityReserved(inventory.getQuantityReserved() - quantity);
        inventoryRepository.save(inventory);
    }

    private Inventory findOrThrow(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found for product: " + productId));
    }

    private InventoryResponse toResponse(Inventory i) {
        return new InventoryResponse(
                i.getId(), i.getProductId(),
                i.getQuantityOnHand(), i.getQuantityReserved(),
                i.getAvailableQuantity(), i.getUpdatedAt());
    }
}
