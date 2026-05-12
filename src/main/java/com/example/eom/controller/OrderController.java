package com.example.eom.controller;

import com.example.eom.dto.ErrorResponseDTO;
import com.example.eom.dto.order.OrderResponse;
import com.example.eom.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement and history for the authenticated customer")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an order from the current cart (snapshots prices, reserves inventory, clears cart)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Cart is empty",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Insufficient stock for one or more items",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public OrderResponse createFromCart(Authentication authentication) {
        return orderService.createFromCart(userId(authentication));
    }

    @GetMapping
    @Operation(summary = "List the current user's orders (newest first)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of orders returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Page<OrderResponse> listMyOrders(Authentication authentication,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return orderService.listByUser(userId(authentication), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific order by ID (must belong to the authenticated user)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Order belongs to a different user",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public OrderResponse getById(Authentication authentication, @PathVariable Long id) {
        return orderService.getById(userId(authentication), id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a PENDING order — releases reserved inventory")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Order belongs to a different user",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Order is not in PENDING status",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public OrderResponse cancel(Authentication authentication, @PathVariable Long id) {
        return orderService.cancel(userId(authentication), id);
    }

    private Long userId(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }
}
