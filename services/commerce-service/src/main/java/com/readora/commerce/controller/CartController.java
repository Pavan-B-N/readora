package com.readora.commerce.controller;

import com.readora.commerce.dto.AddCartItemRequest;
import com.readora.commerce.dto.CartResponse;
import com.readora.commerce.dto.CartSummaryResponse;
import com.readora.commerce.dto.SetCartItemRequest;
import com.readora.commerce.entity.DeliveryType;
import com.readora.commerce.security.CurrentUserContext;
import com.readora.commerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Cart")
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(
            summary = "Get the caller's cart",
            description = "Returns the caller's Redis-backed cart contents, subtotal, and item count, priced at the time each item was added.",
            tags = {"Cart"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart contents returned")
    })
    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        return ResponseEntity.ok(cartService.getCart(CurrentUserContext.require()));
    }

    @Operation(
            summary = "Add an item to the cart",
            description = "Adds a book to the cart, or increments its quantity if it's already present. Validates live stock and the 10-per-title limit against catalog-service.",
            tags = {"Cart"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated cart summary returned"),
            @ApiResponse(responseCode = "404", description = "The book does not exist or is no longer active"),
            @ApiResponse(responseCode = "409", description = "Requested quantity exceeds available inventory"),
            @ApiResponse(responseCode = "422", description = "More than 10 of a single title requested")
    })
    @PostMapping("/items")
    public ResponseEntity<CartSummaryResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(CurrentUserContext.require(), request));
    }

    @Operation(
            summary = "Set a line item's quantity",
            description = "Sets the absolute quantity of one cart line item. A quantity of 0 removes it from the cart.",
            tags = {"Cart"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated cart summary returned"),
            @ApiResponse(responseCode = "404", description = "That book is not in the cart"),
            @ApiResponse(responseCode = "409", description = "Requested quantity exceeds available inventory")
    })
    @PutMapping("/items/{bookId}/{deliveryType}")
    public ResponseEntity<CartSummaryResponse> setItemQty(
            @PathVariable UUID bookId, @PathVariable DeliveryType deliveryType, @Valid @RequestBody SetCartItemRequest request
    ) {
        return ResponseEntity.ok(cartService.setItemQty(CurrentUserContext.require(), bookId, deliveryType, request.qty()));
    }
}
