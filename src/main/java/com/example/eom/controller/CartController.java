package com.example.eom.controller;

import com.example.eom.dto.ErrorResponseDTO;
import com.example.eom.dto.cart.AddToCartRequest;
import com.example.eom.dto.cart.CartResponse;
import com.example.eom.dto.cart.UpdateCartItemRequest;
import com.example.eom.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management — one cart per authenticated user")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get the current user's cart with totals")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart returned",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public CartResponse getCart(Authentication authentication) {
        return cartService.getCart(userId(authentication));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product to the cart — increments quantity if already present")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item added, updated cart returned",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found or inactive",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Insufficient stock",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public CartResponse addItem(Authentication authentication,
                                @Valid @RequestBody AddToCartRequest request) {
        return cartService.addItem(userId(authentication), request);
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update quantity of a cart item — set to 0 to remove")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantity updated, cart returned",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Item not in cart",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public CartResponse updateItem(Authentication authentication,
                                   @PathVariable Long productId,
                                   @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(userId(authentication), productId, request);
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove a specific item from the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item removed, updated cart returned",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Item not in cart",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public CartResponse removeItem(Authentication authentication,
                                   @PathVariable Long productId) {
        return cartService.removeItem(userId(authentication), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Clear all items from the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cart cleared"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public void clearCart(Authentication authentication) {
        cartService.clearCart(userId(authentication));
    }

    private Long userId(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }
}
