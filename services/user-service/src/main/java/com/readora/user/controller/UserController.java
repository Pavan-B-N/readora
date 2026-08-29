package com.readora.user.controller;

import com.readora.user.dto.AddressResponse;
import com.readora.user.dto.BrowsingHistoryItemResponse;
import com.readora.user.dto.CreateAddressRequest;
import com.readora.user.dto.CreateAddressResponse;
import com.readora.user.dto.MeResponse;
import com.readora.user.dto.RedeemCouponRequest;
import com.readora.user.dto.RedeemCouponResponse;
import com.readora.user.dto.TopUpRequest;
import com.readora.user.dto.UpdateProfileRequest;
import com.readora.user.dto.WalletBalanceResponse;
import com.readora.user.dto.WalletResponse;
import com.readora.user.dto.WishlistItemResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
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
            summary = "Update the caller's profile",
            description = "Updates display name, phone, preferred store, and favorite categories. Every field is applied — send the current value for anything you don't intend to change.",
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated")
    })
    @PutMapping
    public ResponseEntity<MeResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        UUID userId = CurrentUserContext.require();
        String email = CurrentUserContext.getEmail().orElse(null);
        return ResponseEntity.ok(userService.updateProfile(userId, email, request));
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
            summary = "Set an address as default",
            description = "Marks one address default and clears the flag from any other address that held it.",
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Default address updated"),
            @ApiResponse(responseCode = "404", description = "No such address belonging to the caller")
    })
    @PutMapping("/addresses/{id}/default")
    public ResponseEntity<Void> setDefaultAddress(@PathVariable UUID id) {
        userService.setDefaultAddress(CurrentUserContext.require(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List wishlist items",
            description = "Lists the caller's wishlisted book ids, newest-first. Just ids — the caller looks up display data via catalog-service's batch lookup.",
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wishlist returned")
    })
    @GetMapping("/wishlist")
    public ResponseEntity<List<WishlistItemResponse>> listWishlist() {
        return ResponseEntity.ok(userService.listWishlist(CurrentUserContext.require()));
    }

    @Operation(
            summary = "Add a book to the wishlist",
            description = "Idempotent — adding a book already on the wishlist is a no-op.",
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Book is on the wishlist")
    })
    @PutMapping("/wishlist/{bookId}")
    public ResponseEntity<Void> addToWishlist(@PathVariable UUID bookId) {
        userService.addToWishlist(CurrentUserContext.require(), bookId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Remove a book from the wishlist",
            description = "Idempotent — removing a book that isn't on the wishlist is a no-op.",
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Book is off the wishlist")
    })
    @DeleteMapping("/wishlist/{bookId}")
    public ResponseEntity<Void> removeFromWishlist(@PathVariable UUID bookId) {
        userService.removeFromWishlist(CurrentUserContext.require(), bookId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List recently viewed books",
            description = "The caller's last 20 viewed book ids, most-recently-viewed first. Just ids — the caller looks up display data via catalog-service's batch lookup.",
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Browsing history returned")
    })
    @GetMapping("/history")
    public ResponseEntity<List<BrowsingHistoryItemResponse>> listBrowsingHistory() {
        return ResponseEntity.ok(userService.listBrowsingHistory(CurrentUserContext.require()));
    }

    @Operation(
            summary = "Record a book view",
            description = "Idempotent upsert — viewing a book already in history just bumps it back to the top rather than duplicating it.",
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "View recorded")
    })
    @PutMapping("/history/{bookId}")
    public ResponseEntity<Void> recordBookView(@PathVariable UUID bookId) {
        userService.recordBookView(CurrentUserContext.require(), bookId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Top up the caller's wallet",
            description = "Dummy top-up — credits the wallet directly since there's no real payment gateway in this build. Exists so an insufficient-balance checkout has somewhere to send the user.",
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wallet credited, new balance returned")
    })
    @PostMapping("/wallet/topup")
    public ResponseEntity<WalletBalanceResponse> topUp(@Valid @RequestBody TopUpRequest request) {
        return ResponseEntity.ok(userService.topUp(CurrentUserContext.require(), request.amount()));
    }

    @Operation(
            summary = "Redeem a coupon code",
            description = "Credits the wallet by the coupon's amount — once per user per coupon, Amazon-Pay-style.",
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon redeemed, wallet credited"),
            @ApiResponse(responseCode = "404", description = "No such coupon code"),
            @ApiResponse(responseCode = "409", description = "The coupon is expired/inactive, or already redeemed by this caller")
    })
    @PostMapping("/wallet/redeem-coupon")
    public ResponseEntity<RedeemCouponResponse> redeemCoupon(@Valid @RequestBody RedeemCouponRequest request) {
        return ResponseEntity.ok(userService.redeemCoupon(CurrentUserContext.require(), request.code()));
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
