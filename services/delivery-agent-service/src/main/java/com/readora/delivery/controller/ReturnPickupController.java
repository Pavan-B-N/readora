package com.readora.delivery.controller;

import com.readora.delivery.dto.ReturnPickupDetailResponse;
import com.readora.delivery.dto.ReturnPickupResponse;
import com.readora.delivery.security.CurrentUserContext;
import com.readora.delivery.service.ReturnPickupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Mirrors DeliveryController's shape exactly, for the reverse (pickup) leg of a physical return. Requires the DELIVERY_AGENT role. */
@Tag(name = "Returns")
@RestController
@RequestMapping("/api/v1/returns")
public class ReturnPickupController {

    private final ReturnPickupService returnPickupService;

    public ReturnPickupController(ReturnPickupService returnPickupService) {
        this.returnPickupService = returnPickupService;
    }

    @Operation(
            summary = "List unassigned return pickups at the caller's store",
            description = "The shared claim queue — any agent at this store can accept one of these.",
            tags = {"Returns"}
    )
    @GetMapping("/queue")
    public ResponseEntity<List<ReturnPickupResponse>> queue() {
        return ResponseEntity.ok(returnPickupService.getQueue(CurrentUserContext.require()));
    }

    @Operation(summary = "List the caller's own claimed return pickups, most recent first", tags = {"Returns"})
    @GetMapping("/mine")
    public ResponseEntity<List<ReturnPickupResponse>> mine() {
        return ResponseEntity.ok(returnPickupService.getMine(CurrentUserContext.require()));
    }

    @Operation(
            summary = "Get a pickup plus the order's full delivery detail",
            description = "Includes the full shipping address needed to go collect the book.",
            tags = {"Returns"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detail returned"),
            @ApiResponse(responseCode = "404", description = "No such pickup at the caller's store")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReturnPickupDetailResponse> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(returnPickupService.getDetail(CurrentUserContext.require(), id));
    }

    @Operation(
            summary = "Claim an unassigned return pickup",
            description = "First come, first served — 409 if another agent already claimed it.",
            tags = {"Returns"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claimed"),
            @ApiResponse(responseCode = "404", description = "No such pickup at the caller's store"),
            @ApiResponse(responseCode = "409", description = "Already claimed by another agent")
    })
    @PostMapping("/{id}/claim")
    public ResponseEntity<ReturnPickupResponse> claim(@PathVariable UUID id) {
        return ResponseEntity.ok(returnPickupService.claim(CurrentUserContext.require(), id));
    }

    @Operation(summary = "Mark a claimed pickup as on the way", tags = {"Returns"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "No such pickup claimed by the caller"),
            @ApiResponse(responseCode = "409", description = "The pickup isn't ASSIGNED")
    })
    @PostMapping("/{id}/en-route")
    public ResponseEntity<ReturnPickupResponse> enRoute(@PathVariable UUID id) {
        return ResponseEntity.ok(returnPickupService.markEnRoute(CurrentUserContext.require(), id));
    }

    @Operation(summary = "Mark a claimed pickup's book collected", tags = {"Returns"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "No such pickup claimed by the caller"),
            @ApiResponse(responseCode = "409", description = "The pickup isn't EN_ROUTE")
    })
    @PostMapping("/{id}/collected")
    public ResponseEntity<ReturnPickupResponse> collected(@PathVariable UUID id) {
        return ResponseEntity.ok(returnPickupService.markCollected(CurrentUserContext.require(), id));
    }
}
