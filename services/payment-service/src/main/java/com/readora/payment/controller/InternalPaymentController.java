package com.readora.payment.controller;

import com.readora.payment.dto.PaymentResponse;
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

/**
 * Service-to-service only — protected by the shared gateway secret, no caller-identity check
 * (see GatewaySecretFilter / SecurityProperties.public-routes). Ownership of the order is already
 * enforced by commerce-service, the only caller, before it ever reaches here — unlike
 * PaymentController's public /api/v1/payments/{orderId}, which authenticates the caller but
 * doesn't check they own the order being asked about.
 */
@Tag(name = "Internal")
@RestController
@RequestMapping("/internal/payments")
public class InternalPaymentController {

    private final PaymentService paymentService;

    public InternalPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(
            summary = "Get payment details for an order",
            description = "Called by commerce-service to enrich its order-detail response with transaction info.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment details returned"),
            @ApiResponse(responseCode = "404", description = "No payment recorded for that order yet")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponse> getByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getByOrderId(orderId));
    }
}
