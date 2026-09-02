package com.readora.commerce.controller;

import com.readora.commerce.dto.OrderDeliveryDetailResponse;
import com.readora.commerce.dto.UpdateDeliveryStatusRequest;
import com.readora.commerce.dto.UpdateReturnStatusRequest;
import com.readora.commerce.service.OrderFulfillmentService;
import com.readora.commerce.service.OrderQueryService;
import com.readora.commerce.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Internal")
@RestController
@RequestMapping("/internal/orders")
public class InternalDeliveryController {

    private final OrderQueryService orderQueryService;
    private final OrderFulfillmentService orderFulfillmentService;
    private final ReturnService returnService;

    public InternalDeliveryController(
            OrderQueryService orderQueryService, OrderFulfillmentService orderFulfillmentService, ReturnService returnService
    ) {
        this.orderQueryService = orderQueryService;
        this.orderFulfillmentService = orderFulfillmentService;
        this.returnService = returnService;
    }

    @Operation(
            summary = "Get an order's full delivery detail",
            description = "Internal, service-to-service only — protected by the shared gateway secret. Called by delivery-agent-service to show an agent everything needed to fulfill a physical order, including the full shipping address.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery detail returned"),
            @ApiResponse(responseCode = "404", description = "No such order")
    })
    @GetMapping("/{id}/delivery-detail")
    public ResponseEntity<OrderDeliveryDetailResponse> getDeliveryDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(orderQueryService.getDeliveryDetail(id));
    }

    @Operation(
            summary = "Advance an order's delivery status",
            description = "Internal, service-to-service only — protected by the shared gateway secret. Called by delivery-agent-service as an agent claims, ships, and delivers a physical order. Enforces CONFIRMED -> ASSIGNED -> SHIPPED -> DELIVERED with no skipped steps.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Status updated"),
            @ApiResponse(responseCode = "404", description = "No such order"),
            @ApiResponse(responseCode = "409", description = "The requested status can't follow the order's current status")
    })
    @PutMapping("/{id}/delivery-status")
    public ResponseEntity<Void> updateDeliveryStatus(@PathVariable UUID id, @Valid @RequestBody UpdateDeliveryStatusRequest request) {
        orderFulfillmentService.updateDeliveryStatus(id, request.status(), request.deliveryAgentId(), request.deliveryAgentName());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Advance an order's return-pickup status",
            description = "Internal, service-to-service only — protected by the shared gateway secret. Called by delivery-agent-service as an agent claims, travels to, and collects a return pickup. Enforces RETURN_APPROVED -> RETURN_ASSIGNED -> RETURN_EN_ROUTE -> RETURN_COLLECTED with no skipped steps; reaching RETURN_COLLECTED also kicks off the refund.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Status updated"),
            @ApiResponse(responseCode = "404", description = "No such order"),
            @ApiResponse(responseCode = "409", description = "The requested status can't follow the order's current status")
    })
    @PutMapping("/{id}/return-status")
    public ResponseEntity<Void> updateReturnStatus(@PathVariable UUID id, @Valid @RequestBody UpdateReturnStatusRequest request) {
        returnService.updateReturnPickupStatus(id, request.status(), request.returnAgentId(), request.returnAgentName());
        return ResponseEntity.noContent().build();
    }
}
