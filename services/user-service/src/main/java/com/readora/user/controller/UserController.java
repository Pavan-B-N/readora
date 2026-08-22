package com.readora.user.controller;

import com.readora.user.dto.AddressResponse;
import com.readora.user.dto.CreateAddressRequest;
import com.readora.user.dto.CreateAddressResponse;
import com.readora.user.dto.MeResponse;
import com.readora.user.dto.WalletResponse;
import com.readora.user.security.CurrentUserContext;
import com.readora.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "User")
@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Get the caller's profile",
            description = "Returns the caller's display profile together with their wallet balance. The profile and wallet are provisioned automatically (with a signup bonus) the first time this is called for a given user.",
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile and wallet balance returned")
    })
    @GetMapping
    public ResponseEntity<MeResponse> me() {
        UUID userId = CurrentUserContext.require();
        String email = CurrentUserContext.getEmail().orElse(null);
        return ResponseEntity.ok(userService.getMe(userId, email));
    }

    @Operation(
            summary = "List addresses",
            description = "Lists the caller's non-deleted addresses.",
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address list returned")
    })
    @GetMapping("/addresses")
    public ResponseEntity<List<AddressResponse>> listAddresses() {
        return ResponseEntity.ok(userService.listAddresses(CurrentUserContext.require()));
    }

    @Operation(
            summary = "Add an address",
            description = "Adds a new address to the caller's address book.",
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address created"),
            @ApiResponse(responseCode = "400", description = "Missing required address fields"),
            @ApiResponse(responseCode = "409", description = "The account already holds the maximum of 20 addresses")
    })
    @PostMapping("/addresses")
    public ResponseEntity<CreateAddressResponse> addAddress(@Valid @RequestBody CreateAddressRequest request) {
        CreateAddressResponse response = userService.addAddress(CurrentUserContext.require(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Delete an address",
            description = "Soft-deletes an address — orders that already shipped to it keep their own immutable snapshot.",
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Address deleted"),
            @ApiResponse(responseCode = "404", description = "No such address belonging to the caller")
    })
    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable UUID id) {
        userService.deleteAddress(CurrentUserContext.require(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get wallet balance and ledger",
            description = "Returns the caller's current wallet balance and a paginated, newest-first transaction ledger.",
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wallet balance and ledger page returned")
    })
    @GetMapping("/wallet")
    public ResponseEntity<WalletResponse> wallet(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(userService.getWallet(CurrentUserContext.require(), pageable));
    }
}
