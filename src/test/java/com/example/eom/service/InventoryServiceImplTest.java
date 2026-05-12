package com.example.eom.service;

import com.example.eom.domain.Inventory;
import com.example.eom.dto.inventory.InventoryResponse;
import com.example.eom.dto.inventory.UpdateInventoryRequest;
import com.example.eom.exception.InsufficientStockException;
import com.example.eom.repository.InventoryRepository;
import com.example.eom.service.impl.InventoryServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock InventoryRepository inventoryRepository;
    @InjectMocks InventoryServiceImpl inventoryService;

    private Inventory inventory(int onHand, int reserved) {
        Inventory i = new Inventory();
        i.setId(1L);
        i.setProductId(10L);
        i.setQuantityOnHand(onHand);
        i.setQuantityReserved(reserved);
        i.setUpdatedAt(Instant.now());
        return i;
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_noFilter_returnsAll() {
        given(inventoryRepository.findAll()).willReturn(List.of(inventory(50, 5)));
        List<InventoryResponse> result = inventoryService.list(null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).availableQuantity()).isEqualTo(45);
    }

    @Test
    void list_withProductId_returnsFiltered() {
        given(inventoryRepository.findByProductId(10L)).willReturn(Optional.of(inventory(50, 5)));
        List<InventoryResponse> result = inventoryService.list(10L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).productId()).isEqualTo(10L);
    }

    // ── updateStock ───────────────────────────────────────────────────────────

    @Test
    void updateStock_existingRecord_setsOnHand() {
        Inventory existing = inventory(50, 5);
        given(inventoryRepository.findByProductIdWithLock(10L)).willReturn(Optional.of(existing));
        given(inventoryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        InventoryResponse response = inventoryService.updateStock(10L, new UpdateInventoryRequest(100));

        assertThat(response.quantityOnHand()).isEqualTo(100);
        assertThat(response.availableQuantity()).isEqualTo(95);
    }

    @Test
    void updateStock_belowReserved_throwsIllegalArgument() {
        Inventory existing = inventory(50, 20);
        given(inventoryRepository.findByProductIdWithLock(10L)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> inventoryService.updateStock(10L, new UpdateInventoryRequest(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("below current reserved");
    }

    // ── reserve ───────────────────────────────────────────────────────────────

    @Test
    void reserve_sufficientStock_incrementsReserved() {
        Inventory inv = inventory(50, 5);
        given(inventoryRepository.findByProductIdWithLock(10L)).willReturn(Optional.of(inv));
        given(inventoryRepository.save(any())).willAnswer(a -> a.getArgument(0));

        inventoryService.reserve(10L, 10);

        assertThat(inv.getQuantityReserved()).isEqualTo(15);
        verify(inventoryRepository).save(inv);
    }

    @Test
    void reserve_insufficientStock_throwsInsufficientStock() {
        given(inventoryRepository.findByProductIdWithLock(10L)).willReturn(Optional.of(inventory(10, 8)));

        assertThatThrownBy(() -> inventoryService.reserve(10L, 5))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("requested 5, available 2");
    }

    @Test
    void reserve_noInventory_throwsNotFound() {
        given(inventoryRepository.findByProductIdWithLock(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.reserve(99L, 1))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── deduct ────────────────────────────────────────────────────────────────

    @Test
    void deduct_decrementsOnHandAndReserved() {
        Inventory inv = inventory(50, 10);
        given(inventoryRepository.findByProductIdWithLock(10L)).willReturn(Optional.of(inv));
        given(inventoryRepository.save(any())).willAnswer(a -> a.getArgument(0));

        inventoryService.deduct(10L, 10);

        assertThat(inv.getQuantityOnHand()).isEqualTo(40);
        assertThat(inv.getQuantityReserved()).isEqualTo(0);
    }

    // ── release ───────────────────────────────────────────────────────────────

    @Test
    void release_decrementsReservedOnly() {
        Inventory inv = inventory(50, 10);
        given(inventoryRepository.findByProductIdWithLock(10L)).willReturn(Optional.of(inv));
        given(inventoryRepository.save(any())).willAnswer(a -> a.getArgument(0));

        inventoryService.release(10L, 5);

        assertThat(inv.getQuantityReserved()).isEqualTo(5);
        assertThat(inv.getQuantityOnHand()).isEqualTo(50);
    }
}
