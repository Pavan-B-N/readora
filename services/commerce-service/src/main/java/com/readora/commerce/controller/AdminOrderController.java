package com.readora.commerce.controller;

import com.readora.commerce.dto.AdminOrderSummaryResponse;
import com.readora.commerce.dto.PostReturnMessageRequest;
import com.readora.commerce.dto.ReturnMessageResponse;
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

import java.util.List;
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
            summary = "One return/cancellation case's full detail",
            description = "Backs the dedicated review page — same fields as the list row, fetched directly by id so a refresh or a shared link still works.",
            tags = {"Admin Orders"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Case detail returned"),
            @ApiResponse(responseCode = "404", description = "No such order at the caller's store")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AdminOrderSummaryResponse> getReturn(@PathVariable UUID id) {
        return ResponseEntity.ok(adminOrderService.getReturn(id));
    }

    @Operation(
            summary = "Review a return/cancellation case",
            description = "Records an internal note. If decision (\"APPROVE\"/\"REJECT\") is present and the order is RETURN_REQUESTED, also decides the return — approving queues a delivery-agent pickup, rejecting is terminal. Omitting decision keeps the plain acknowledgement behavior (e.g. for a cancelled order, which has nothing to decide).",
            tags = {"Admin Orders"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Case reviewed"),
            @ApiResponse(responseCode = "404", description = "No such order at the caller's store"),
            @ApiResponse(responseCode = "409", description = "decision was given but the order isn't awaiting review")
    })
    @PostMapping("/{id}/review")
    public ResponseEntity<AdminOrderSummaryResponse> review(@PathVariable UUID id, @Valid @RequestBody ReviewOrderRequest request) {
        return ResponseEntity.ok(adminOrderService.reviewOrder(id, request.note(), request.decision()));
    }

    @Operation(
            summary = "Get the return chat for an order",
            description = "The small back-and-forth between an admin and the customer while a return sits at RETURN_REQUESTED. Always readable, even after the return is decided.",
            tags = {"Admin Orders"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages returned (possibly empty)"),
            @ApiResponse(responseCode = "404", description = "No such order at the caller's store")
    })
    @GetMapping("/{id}/return/messages")
    public ResponseEntity<List<ReturnMessageResponse>> returnMessages(@PathVariable UUID id) {
        return ResponseEntity.ok(adminOrderService.listReturnMessages(id));
    }

    @Operation(
            summary = "Send a return chat message",
            description = "Only accepted while the return is RETURN_REQUESTED — locked once the case is approved or rejected.",
            tags = {"Admin Orders"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message sent"),
            @ApiResponse(responseCode = "404", description = "No such order at the caller's store"),
            @ApiResponse(responseCode = "409", description = "The return isn't awaiting review anymore")
    })
    @PostMapping("/{id}/return/messages")
    public ResponseEntity<ReturnMessageResponse> postReturnMessage(@PathVariable UUID id, @Valid @RequestBody PostReturnMessageRequest request) {
        return ResponseEntity.ok(adminOrderService.postReturnMessage(id, request.content()));
    }
}
