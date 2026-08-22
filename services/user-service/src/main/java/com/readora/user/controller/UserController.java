package com.readora.user.controller;

import com.readora.user.dto.AddressResponse;
import com.readora.user.dto.CreateAddressRequest;
import com.readora.user.dto.CreateAddressResponse;
import com.readora.user.dto.MeResponse;
import com.readora.user.dto.WalletResponse;
import com.readora.user.security.CurrentUserContext;
import com.readora.user.service.UserService;
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

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<MeResponse> me() {
        UUID userId = CurrentUserContext.require();
        String email = CurrentUserContext.getEmail().orElse(null);
        return ResponseEntity.ok(userService.getMe(userId, email));
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressResponse>> listAddresses() {
        return ResponseEntity.ok(userService.listAddresses(CurrentUserContext.require()));
    }

    @PostMapping("/addresses")
    public ResponseEntity<CreateAddressResponse> addAddress(@Valid @RequestBody CreateAddressRequest request) {
        CreateAddressResponse response = userService.addAddress(CurrentUserContext.require(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable UUID id) {
        userService.deleteAddress(CurrentUserContext.require(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/wallet")
    public ResponseEntity<WalletResponse> wallet(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(userService.getWallet(CurrentUserContext.require(), pageable));
    }
}
