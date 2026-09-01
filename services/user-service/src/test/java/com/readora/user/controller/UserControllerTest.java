package com.readora.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.readora.user.dto.AddressResponse;
import com.readora.user.dto.BrowsingHistoryItemResponse;
import com.readora.user.dto.CreateAddressRequest;
import com.readora.user.dto.CreateAddressResponse;
import com.readora.user.dto.MeResponse;
import com.readora.user.dto.RecordSearchRequest;
import com.readora.user.dto.RedeemCouponRequest;
import com.readora.user.dto.RedeemCouponResponse;
import com.readora.user.dto.SearchHistoryItemResponse;
import com.readora.user.dto.TopUpRequest;
import com.readora.user.dto.WalletBalanceResponse;
import com.readora.user.dto.WalletResponse;
import com.readora.user.dto.WishlistItemResponse;
import com.readora.user.entity.AddressLabel;
import com.readora.user.entity.AddressRecipientType;
import com.readora.user.exception.AddressNotFoundException;
import com.readora.user.exception.GlobalExceptionHandler;
import com.readora.user.security.CurrentUserContext;
import com.readora.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        CurrentUserContext.set(userId, "reader@example.com");
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void me_returnsTheServiceResponse() throws Exception {
        when(userService.getMe(eq(userId), eq("reader@example.com"))).thenReturn(
                new MeResponse(userId, "reader@example.com", "Name", null, null, "en-IN", null, null,
                        List.of(), new MeResponse.WalletSummary(new BigDecimal("500.00"), "INR")));

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("reader@example.com"))
                .andExpect(jsonPath("$.wallet.balance").value(500.00));
    }

    @Test
    void addAddress_valid_returns201() throws Exception {
        when(userService.addAddress(eq(userId), any())).thenReturn(new CreateAddressResponse(UUID.randomUUID(), true));

        CreateAddressRequest request = new CreateAddressRequest(
                AddressLabel.HOME, AddressRecipientType.OWNER, "A Name", "9999999999",
                "Line 1", null, "City", "State", "000000", "IN", null, true);

        mockMvc.perform(post("/api/v1/users/me/addresses")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    void addAddress_missingRequiredField_returns400() throws Exception {
        CreateAddressRequest request = new CreateAddressRequest(
                null, AddressRecipientType.OWNER, "A Name", "9999999999",
                "Line 1", null, "City", "State", "000000", "IN", null, true);

        mockMvc.perform(post("/api/v1/users/me/addresses")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void deleteAddress_notFound_mapsServiceExceptionTo404() throws Exception {
        org.mockito.Mockito.doThrow(new AddressNotFoundException()).when(userService).deleteAddress(eq(userId), any());

        mockMvc.perform(delete("/api/v1/users/me/addresses/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ADDRESS_NOT_FOUND"));
    }

    @Test
    void deleteAddress_found_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/addresses/" + UUID.randomUUID()))
                .andExpect(status().isNoContent());

        verify(userService).deleteAddress(eq(userId), any());
    }

    @Test
    void addToWishlist_returns204() throws Exception {
        UUID bookId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/users/me/wishlist/" + bookId))
                .andExpect(status().isNoContent());

        verify(userService).addToWishlist(userId, bookId);
    }

    @Test
    void recordSearch_blankQuery_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/search-history")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RecordSearchRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void topUp_amountBelowMinimum_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/wallet/topup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TopUpRequest(new BigDecimal("0.50")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redeemCoupon_blankCode_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/wallet/redeem-coupon")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RedeemCouponRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_returns200WithUpdatedResponse() throws Exception {
        when(userService.updateProfile(eq(userId), eq("reader@example.com"), any())).thenReturn(
                new MeResponse(userId, "reader@example.com", "New Name", null, null, "en-IN", null, null,
                        List.of(), new MeResponse.WalletSummary(BigDecimal.ZERO, "INR")));

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new com.readora.user.dto.UpdateProfileRequest("New Name", null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("New Name"));
    }

    @Test
    void listAddresses_returnsTheServiceList() throws Exception {
        when(userService.listAddresses(userId)).thenReturn(List.of(
                new AddressResponse(UUID.randomUUID(), AddressLabel.HOME, AddressRecipientType.OWNER, "A", "999",
                        "L1", null, "City", "State", "000000", "IN", null, true)));

        mockMvc.perform(get("/api/v1/users/me/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("City"));
    }

    @Test
    void setDefaultAddress_returns204() throws Exception {
        UUID addressId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/users/me/addresses/" + addressId + "/default"))
                .andExpect(status().isNoContent());

        verify(userService).setDefaultAddress(userId, addressId);
    }

    @Test
    void removeFromWishlist_returns204() throws Exception {
        UUID bookId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/users/me/wishlist/" + bookId))
                .andExpect(status().isNoContent());

        verify(userService).removeFromWishlist(userId, bookId);
    }

    @Test
    void listWishlist_returnsTheServiceList() throws Exception {
        UUID bookId = UUID.randomUUID();
        when(userService.listWishlist(userId)).thenReturn(List.of(new WishlistItemResponse(bookId, Instant.now())));

        mockMvc.perform(get("/api/v1/users/me/wishlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookId").value(bookId.toString()));
    }

    @Test
    void recordBookView_returns204() throws Exception {
        UUID bookId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/users/me/history/" + bookId))
                .andExpect(status().isNoContent());

        verify(userService).recordBookView(userId, bookId);
    }

    @Test
    void listBrowsingHistory_returnsTheServiceList() throws Exception {
        UUID bookId = UUID.randomUUID();
        when(userService.listBrowsingHistory(userId)).thenReturn(List.of(new BrowsingHistoryItemResponse(bookId, Instant.now())));

        mockMvc.perform(get("/api/v1/users/me/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookId").value(bookId.toString()));
    }

    @Test
    void listSearchHistory_returnsTheServiceList() throws Exception {
        when(userService.listSearchHistory(userId)).thenReturn(List.of(new SearchHistoryItemResponse("spring boot", Instant.now())));

        mockMvc.perform(get("/api/v1/users/me/search-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].query").value("spring boot"));
    }

    @Test
    void recordSearch_valid_returns204() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/search-history")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RecordSearchRequest("spring boot"))))
                .andExpect(status().isNoContent());

        verify(userService).recordSearch(userId, "spring boot");
    }

    @Test
    void topUp_valid_returns200WithNewBalance() throws Exception {
        when(userService.topUp(eq(userId), any())).thenReturn(new WalletBalanceResponse(new BigDecimal("600.00"), "INR"));

        mockMvc.perform(post("/api/v1/users/me/wallet/topup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TopUpRequest(new BigDecimal("100.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(600.00));
    }

    @Test
    void redeemCoupon_valid_returns200() throws Exception {
        when(userService.redeemCoupon(eq(userId), eq("WELCOME10"))).thenReturn(
                new RedeemCouponResponse(new BigDecimal("50.00"), new BigDecimal("550.00"), "INR"));

        mockMvc.perform(post("/api/v1/users/me/wallet/redeem-coupon")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RedeemCouponRequest("WELCOME10"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditedAmount").value(50.00));
    }

    @Test
    void wallet_defaultPagination_returnsBalanceAndLedger() throws Exception {
        when(userService.getWallet(eq(userId), any())).thenReturn(
                new WalletResponse(new BigDecimal("500.00"), "INR", List.of()));

        mockMvc.perform(get("/api/v1/users/me/wallet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00));
    }

    @Test
    void unexpectedException_mapsTo500WithoutLeakingDetail() throws Exception {
        when(userService.getMe(any(), any())).thenThrow(new RuntimeException("db down"));

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }
}
