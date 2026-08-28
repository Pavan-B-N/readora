package com.readora.delivery.controller;

import com.readora.delivery.dto.AdminAgentResponse;
import com.readora.delivery.service.AdminDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Requires the ADMIN role — enforced by UserContextFilter's /api/v1/admin/** gate. */
@Tag(name = "Admin Delivery")
@RestController
@RequestMapping("/api/v1/admin/delivery")
public class AdminDeliveryController {

    private final AdminDeliveryService adminDeliveryService;

    public AdminDeliveryController(AdminDeliveryService adminDeliveryService) {
        this.adminDeliveryService = adminDeliveryService;
    }

    @Operation(
            summary = "List delivery agents at the caller's store",
            description = "Who's on duty and what they're carrying right now (a delivery or a return pickup), if anything.",
            tags = {"Admin Delivery"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agents returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin, or has no store assignment")
    })
    @GetMapping("/agents")
    public ResponseEntity<List<AdminAgentResponse>> listAgents() {
        return ResponseEntity.ok(adminDeliveryService.listAgents());
    }
}
