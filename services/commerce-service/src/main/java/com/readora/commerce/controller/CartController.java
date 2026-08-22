package com.readora.commerce.controller;

import com.readora.commerce.dto.AddCartItemRequest;
import com.readora.commerce.dto.CartResponse;
import com.readora.commerce.dto.CartSummaryResponse;
import com.readora.commerce.dto.SetCartItemRequest;
import com.readora.commerce.security.CurrentUserContext;
import com.readora.commerce.service.CartService;
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

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        return ResponseEntity.ok(cartService.getCart(CurrentUserContext.require()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartSummaryResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(CurrentUserContext.require(), request));
    }

    @PutMapping("/items/{bookId}")
    public ResponseEntity<CartSummaryResponse> setItemQty(@PathVariable UUID bookId, @Valid @RequestBody SetCartItemRequest request) {
        return ResponseEntity.ok(cartService.setItemQty(CurrentUserContext.require(), bookId, request.qty()));
    }
}
