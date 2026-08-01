package com.readora.payment.controller;

import com.readora.payment.dto.PaymentResponse;
import com.readora.sharedcore.security.CurrentUserContext;
import com.readora.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Public read-only payment status endpoints, scoped to payments the authenticated caller owns.
@Tag(name = "Payments")
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    // Injects the payment service used to fulfill this endpoint.
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(
            summary = "Get payment status for an order",
            description = "Payments are created by consuming order.created off Kafka, not via a public POST — this is a read-only status lookup, scoped to payments the caller owns.",
            tags = {"Payments"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment status returned"),
            @ApiResponse(responseCode = "404", description = "No payment recorded for that order yet, or it belongs to another user")
    })
    // Returns payment status for the order if the current caller owns it.
    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponse> getByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getByOrderId(orderId, CurrentUserContext.require()));
    }
}
