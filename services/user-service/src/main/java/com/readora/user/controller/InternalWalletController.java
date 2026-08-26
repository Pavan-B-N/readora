package com.readora.user.controller;

import com.readora.user.dto.WalletBalanceResponse;
import com.readora.user.service.UserService;
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

@Tag(name = "Internal")
@RestController
@RequestMapping("/internal/wallet")
public class InternalWalletController {

    private final UserService userService;

    public InternalWalletController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Get a user's current wallet balance",
            description = "Internal, service-to-service only — protected by the shared gateway secret, not a user's JWT. Called by commerce-service at checkout to verify sufficient balance before creating a wallet-funded order.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance returned")
    })
    @GetMapping("/{userId}/balance")
    public ResponseEntity<WalletBalanceResponse> getBalance(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getBalance(userId));
    }
}
