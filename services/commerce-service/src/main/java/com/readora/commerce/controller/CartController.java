package com.readora.commerce.controller;

import com.readora.commerce.dto.AddCartItemRequest;
import com.readora.commerce.dto.CartResponse;
import com.readora.commerce.dto.CartSummaryResponse;
import com.readora.commerce.dto.SetCartItemRequest;
import com.readora.commerce.security.CurrentUserContext;
import com.readora.commerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "Get the caller's cart")
    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        return ResponseEntity.ok(cartService.getCart(CurrentUserContext.require()));
    }

    @Operation(summary = "Add a book to the cart, or increment its quantity")
    @PostMapping("/items")
    public ResponseEntity<CartSummaryResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(CurrentUserContext.require(), request));
    }

    @Operation(summary = "Set the absolute quantity of a line item (0 removes it)")
    @PutMapping("/items/{bookId}")
    public ResponseEntity<CartSummaryResponse> setItemQty(@PathVariable UUID bookId, @Valid @RequestBody SetCartItemRequest request) {
        return ResponseEntity.ok(cartService.setItemQty(CurrentUserContext.require(), bookId, request.qty()));
    }
}
