package com.readora.commerce.controller;

import com.readora.commerce.dto.AdminOrderSummaryResponse;
import com.readora.commerce.dto.ReviewOrderRequest;
import com.readora.commerce.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Requires the ADMIN role — enforced by UserContextFilter's /api/v1/admin/** gate. */
@Tag(name = "Admin Orders")
@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @Operation(
            summary = "List cancelled/returned orders at the caller's store",
            description = "Store-scoped the same way admin book management is — enriched with refund status from payment-service where available.",
            tags = {"Admin Orders"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated returns list returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin, or has no store assignment")
    })
    @GetMapping
    public ResponseEntity<Page<AdminOrderSummaryResponse>> listReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminOrderService.listReturns(PageRequest.of(page, size)));
    }

    @Operation(
            summary = "Mark a return/cancellation case reviewed",
            description = "Records an internal note and timestamp — the acknowledgement step for a case, since refunds themselves are automatic.",
            tags = {"Admin Orders"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Case marked reviewed"),
            @ApiResponse(responseCode = "404", description = "No such order at the caller's store")
    })
    @PostMapping("/{id}/review")
    public ResponseEntity<AdminOrderSummaryResponse> review(@PathVariable UUID id, @Valid @RequestBody ReviewOrderRequest request) {
        return ResponseEntity.ok(adminOrderService.reviewOrder(id, request.note()));
    }
}
