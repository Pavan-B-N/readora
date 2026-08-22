package com.readora.commerce.controller;

import com.readora.commerce.dto.CancelOrderRequest;
import com.readora.commerce.dto.CancelOrderResponse;
import com.readora.commerce.dto.CheckoutRequest;
import com.readora.commerce.dto.CheckoutResponse;
import com.readora.commerce.dto.OrderDetailResponse;
import com.readora.commerce.dto.OrderSummaryResponse;
import com.readora.commerce.security.CurrentUserContext;
import com.readora.commerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request
    ) {
        CheckoutResponse response = orderService.checkout(CurrentUserContext.require(), idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(orderService.listOrders(CurrentUserContext.require(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailResponse> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getDetail(CurrentUserContext.require(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CancelOrderResponse> cancel(@PathVariable UUID id, @RequestBody CancelOrderRequest request) {
        return ResponseEntity.ok(orderService.cancel(CurrentUserContext.require(), id, request));
    }
}
