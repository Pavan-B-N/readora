package com.readora.auth.controller;

import com.readora.auth.dto.*;
import com.readora.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Register a new account",
            description = "Creates an account with the given email and password, hashing the password before it's ever persisted, and assigns the default CUSTOMER role.",
            tags = {"Authentication"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Malformed email or password below the minimum length"),
            @ApiResponse(responseCode = "409", description = "An account already exists for this email")
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse registerResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(registerResponse);
    }

    @Operation(
            summary = "Log in",
            description = "Exchanges an email and password for a new access token and refresh token pair. Unknown email and wrong password return the identical error so the endpoint can't be used to enumerate registered accounts.",
            tags = {"Authentication"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access and refresh token pair issued"),
            @ApiResponse(responseCode = "401", description = "Email is unknown or password is incorrect"),
            @ApiResponse(responseCode = "423", description = "Account is locked after too many failed login attempts")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse authResponse = authService.login(request);
        return ResponseEntity.ok(authResponse);
    }

    @Operation(
            summary = "Rotate a refresh token",
            description = "Validates and revokes the supplied refresh token, then returns a new access and refresh token pair. If a previously-revoked token is presented (reuse), every other active token for that user is revoked too.",
            tags = {"Authentication"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New access and refresh token pair issued"),
            @ApiResponse(responseCode = "401", description = "Refresh token is unknown, expired, or was already revoked (including reuse of a revoked token)")
    })
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshResponse refreshResponse = authService.refresh(request);
        return ResponseEntity.ok(refreshResponse);
    }

    @Operation(
            summary = "Log out",
            description = "Revokes the supplied refresh token. The caller's access token remains valid until it naturally expires.",
            tags = {"Authentication"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Refresh token revoked (or was already invalid — this endpoint does not reveal which)")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
