package com.example.eom.service;

import com.example.eom.dto.inventory.InventoryResponse;
import com.example.eom.dto.inventory.UpdateInventoryRequest;

import java.util.List;

public interface InventoryService {

    List<InventoryResponse> list(Long productId);

    InventoryResponse getByProduct(Long productId);

    InventoryResponse updateStock(Long productId, UpdateInventoryRequest request);

    void reserve(Long productId, int quantity);

    void deduct(Long productId, int quantity);

    void release(Long productId, int quantity);
}
