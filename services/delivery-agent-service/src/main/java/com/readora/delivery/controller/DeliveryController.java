package com.readora.delivery.controller;

import com.readora.delivery.dto.AgentMeResponse;
import com.readora.delivery.dto.AgentStatsResponse;
import com.readora.delivery.dto.AssignmentDetailResponse;
import com.readora.delivery.dto.AssignmentResponse;
import com.readora.delivery.dto.SetDutyRequest;
import com.readora.sharedcore.security.CurrentUserContext;
import com.readora.delivery.service.AgentStatsService;
import com.readora.delivery.service.DeliveryService;
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

import java.util.List;
import java.util.UUID;

/** Every route here requires the DELIVERY_AGENT role — enforced by UserContextFilter's path-prefix gate. */
@Tag(name = "Delivery")
@RestController
@RequestMapping("/api/v1/delivery")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final AgentStatsService agentStatsService;

    public DeliveryController(DeliveryService deliveryService, AgentStatsService agentStatsService) {
        this.deliveryService = deliveryService;
        this.agentStatsService = agentStatsService;
    }

    @Operation(summary = "Get the caller's agent profile", tags = {"Delivery"})
    @GetMapping("/me")
    public ResponseEntity<AgentMeResponse> me() {
        return ResponseEntity.ok(deliveryService.getMe(CurrentUserContext.require()));
    }

    @Operation(
            summary = "Get the caller's lifetime stats",
            description = "Completed deliveries, completed return pickups, and total earnings across both — for the agent's profile page.",
            tags = {"Delivery"}
    )
    @GetMapping("/me/stats")
    public ResponseEntity<AgentStatsResponse> stats() {
        return ResponseEntity.ok(agentStatsService.getStats(CurrentUserContext.require()));
    }

    @Operation(
            summary = "Go on/off duty",
            description = "Off-duty agents stop seeing new orders in the delivery and return-pickup queues, but keep full access to whatever they've already claimed.",
            tags = {"Delivery"}
    )
    @PutMapping("/me/duty")
    public ResponseEntity<AgentMeResponse> setDuty(@Valid @RequestBody SetDutyRequest request) {
        return ResponseEntity.ok(deliveryService.setOnDuty(CurrentUserContext.require(), request.onDuty()));
    }

    @Operation(
            summary = "List unassigned orders at the caller's store",
            description = "The shared claim queue — any agent at this store can accept one of these.",
            tags = {"Delivery"}
    )
    @GetMapping("/queue")
    public ResponseEntity<List<AssignmentResponse>> queue() {
        return ResponseEntity.ok(deliveryService.getQueue(CurrentUserContext.require()));
    }

    @Operation(summary = "List the caller's own claimed deliveries, most recent first", tags = {"Delivery"})
    @GetMapping("/mine")
    public ResponseEntity<List<AssignmentResponse>> mine() {
        return ResponseEntity.ok(deliveryService.getMine(CurrentUserContext.require()));
    }

    @Operation(
            summary = "Get an assignment plus the order's full delivery detail",
            description = "Includes the full shipping address needed to make the delivery.",
            tags = {"Delivery"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detail returned"),
            @ApiResponse(responseCode = "404", description = "No such assignment at the caller's store")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AssignmentDetailResponse> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(deliveryService.getDetail(CurrentUserContext.require(), id));
    }

    @Operation(
            summary = "Claim an unassigned order",
            description = "First come, first served — 409 if another agent already claimed it.",
            tags = {"Delivery"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claimed"),
            @ApiResponse(responseCode = "404", description = "No such assignment at the caller's store"),
            @ApiResponse(responseCode = "409", description = "Already claimed by another agent")
    })
    @PostMapping("/{id}/claim")
    public ResponseEntity<AssignmentResponse> claim(@PathVariable UUID id) {
        return ResponseEntity.ok(deliveryService.claim(CurrentUserContext.require(), id));
    }

    @Operation(summary = "Mark a claimed order out for delivery", tags = {"Delivery"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "No such assignment claimed by the caller"),
            @ApiResponse(responseCode = "409", description = "The order isn't in ASSIGNED status")
    })
    @PostMapping("/{id}/out-for-delivery")
    public ResponseEntity<AssignmentResponse> outForDelivery(@PathVariable UUID id) {
        return ResponseEntity.ok(deliveryService.markOutForDelivery(CurrentUserContext.require(), id));
    }

    @Operation(summary = "Mark a claimed order delivered", tags = {"Delivery"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "No such assignment claimed by the caller"),
            @ApiResponse(responseCode = "409", description = "The order isn't OUT_FOR_DELIVERY")
    })
    @PostMapping("/{id}/delivered")
    public ResponseEntity<AssignmentResponse> delivered(@PathVariable UUID id) {
        return ResponseEntity.ok(deliveryService.markDelivered(CurrentUserContext.require(), id));
    }
}
