package com.example.eom.controller.admin;

import com.example.eom.dto.ErrorResponseDTO;
import com.example.eom.dto.inventory.InventoryResponse;
import com.example.eom.dto.inventory.UpdateInventoryRequest;
import com.example.eom.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Inventory", description = "Inventory level management (ADMIN only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "List inventory — optionally filter by productId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory records returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<InventoryResponse> list(@RequestParam(required = false) Long productId) {
        return inventoryService.list(productId);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Set stock quantity for a product (creates record if none exists)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory updated",
                    content = @Content(schema = @Schema(implementation = InventoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or below-reserved conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public InventoryResponse updateStock(@PathVariable Long productId,
                                         @Valid @RequestBody UpdateInventoryRequest request) {
        return inventoryService.updateStock(productId, request);
    }
}
