package com.readora.payment.controller;

import com.readora.payment.dto.RefundStatusResponse;
import com.readora.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Internal")
@RestController
@RequestMapping("/internal/refunds")
public class InternalRefundController {

    private final PaymentService paymentService;

    public InternalRefundController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(
            summary = "Get refund status for a batch of orders",
            description = "Internal, service-to-service only — protected by the shared gateway secret. Called by commerce-service to enrich its admin returns view. Orders with no refund row yet are simply absent from the result.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund statuses returned (possibly empty)")
    })
    @GetMapping("/by-order-ids")
    public ResponseEntity<List<RefundStatusResponse>> byOrderIds(@RequestParam List<UUID> orderIds) {
        return ResponseEntity.ok(paymentService.getRefundStatuses(orderIds));
    }
}
