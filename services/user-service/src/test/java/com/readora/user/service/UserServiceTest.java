package com.readora.user.service;

import com.readora.user.dto.AddressResponse;
import com.readora.user.dto.CreateAddressRequest;
import com.readora.user.dto.CreateAddressResponse;
import com.readora.user.dto.MeResponse;
import com.readora.user.dto.RedeemCouponResponse;
import com.readora.user.dto.UpdateProfileRequest;
import com.readora.user.dto.WalletBalanceResponse;
import com.readora.user.entity.Address;
import com.readora.user.entity.AddressLabel;
import com.readora.user.entity.AddressRecipientType;
import com.readora.user.entity.BrowsingHistoryItem;
import com.readora.user.entity.Coupon;
import com.readora.user.entity.CouponRedemption;
import com.readora.user.entity.SearchHistoryItem;
import com.readora.user.entity.UserProfile;
import com.readora.user.entity.WalletAccount;
import com.readora.user.entity.WishlistItem;
import com.readora.user.exception.AddressLimitReachedException;
import com.readora.user.exception.AddressNotFoundException;
import com.readora.user.exception.CouponAlreadyRedeemedException;
import com.readora.user.exception.CouponNotFoundException;
import com.readora.user.exception.CouponNotRedeemableException;
import com.readora.user.repository.AddressRepository;
import com.readora.user.repository.BrowsingHistoryRepository;
import com.readora.user.repository.CouponRedemptionRepository;
import com.readora.user.repository.CouponRepository;
import com.readora.user.repository.SearchHistoryRepository;
import com.readora.user.repository.UserProfileRepository;
import com.readora.user.repository.WalletAccountRepository;
import com.readora.user.repository.WalletTransactionRepository;
import com.readora.user.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserProfileRepository userProfileRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private WalletAccountRepository walletAccountRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private CouponRedemptionRepository couponRedemptionRepository;
    @Mock private WishlistRepository wishlistRepository;
    @Mock private BrowsingHistoryRepository browsingHistoryRepository;
    @Mock private SearchHistoryRepository searchHistoryRepository;

    private UserService userService;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userProfileRepository, addressRepository, walletAccountRepository, walletTransactionRepository,
                couponRepository, couponRedemptionRepository, wishlistRepository, browsingHistoryRepository,
                searchHistoryRepository, new BigDecimal("500.00")
        );
    }

    // ---- Wishlist ----

    @Test
    void listWishlist_mapsRepositoryResultsToResponses() {
        WishlistItem item = new WishlistItem(userId, UUID.randomUUID());
        when(wishlistRepository.findAllByUserIdOrderByAddedAtDesc(userId)).thenReturn(List.of(item));

        List<com.readora.user.dto.WishlistItemResponse> results = userService.listWishlist(userId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).bookId()).isEqualTo(item.getBookId());
    }

    @Test
    void listBrowsingHistory_mapsRepositoryResultsToResponses() {
        BrowsingHistoryItem item = new BrowsingHistoryItem(userId, UUID.randomUUID());
        when(browsingHistoryRepository.findTop20ByUserIdOrderByViewedAtDesc(userId)).thenReturn(List.of(item));

        List<com.readora.user.dto.BrowsingHistoryItemResponse> results = userService.listBrowsingHistory(userId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).bookId()).isEqualTo(item.getBookId());
    }

    @Test
    void listSearchHistory_mapsRepositoryResultsToResponses() {
        SearchHistoryItem item = new SearchHistoryItem(userId, "clean code");
        when(searchHistoryRepository.findTop20ByUserIdOrderBySearchedAtDesc(userId)).thenReturn(List.of(item));

        List<com.readora.user.dto.SearchHistoryItemResponse> results = userService.listSearchHistory(userId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).query()).isEqualTo("clean code");
    }

    @Test
    void getRecentBookViewIds_mapsAndCapsAtTheGivenLimit() {
        UUID book1 = UUID.randomUUID();
        UUID book2 = UUID.randomUUID();
        when(browsingHistoryRepository.findTop20ByUserIdOrderByViewedAtDesc(userId)).thenReturn(
                List.of(new BrowsingHistoryItem(userId, book1), new BrowsingHistoryItem(userId, book2))
        );

        List<UUID> results = userService.getRecentBookViewIds(userId, 1);

        assertThat(results).containsExactly(book1);
    }

    @Test
    void addToWishlist_alreadyPresent_isANoOp() {
        UUID bookId = UUID.randomUUID();
        when(wishlistRepository.existsByUserIdAndBookId(userId, bookId)).thenReturn(true);

        userService.addToWishlist(userId, bookId);

        verify(wishlistRepository, never()).save(any());
    }

    @Test
    void addToWishlist_notPresent_saves() {
        UUID bookId = UUID.randomUUID();
        when(wishlistRepository.existsByUserIdAndBookId(userId, bookId)).thenReturn(false);

        userService.addToWishlist(userId, bookId);

        verify(wishlistRepository).save(any(WishlistItem.class));
    }

    @Test
    void removeFromWishlist_notPresent_isANoOp() {
        UUID bookId = UUID.randomUUID();
        when(wishlistRepository.findByUserIdAndBookId(userId, bookId)).thenReturn(Optional.empty());

        userService.removeFromWishlist(userId, bookId);

        verify(wishlistRepository, never()).delete(any());
    }

    // ---- Search history ----

    @Test
    void recordSearch_blankQuery_isANoOp() {
        userService.recordSearch(userId, "   ");

        verify(searchHistoryRepository, never()).save(any());
    }

    @Test
    void recordSearch_nullQuery_isANoOp() {
        userService.recordSearch(userId, null);

        verify(searchHistoryRepository, never()).save(any());
    }

    @Test
    void recordSearch_newTerm_savesTrimmed() {
        when(searchHistoryRepository.findByUserIdAndQueryIgnoreCase(userId, "spring boot"))
                .thenReturn(Optional.empty());

        userService.recordSearch(userId, "  spring boot  ");

        verify(searchHistoryRepository).save(any(SearchHistoryItem.class));
    }

    @Test
    void recordSearch_existingTerm_touchesRatherThanDuplicating() {
        SearchHistoryItem existing = new SearchHistoryItem(userId, "spring boot");
        when(searchHistoryRepository.findByUserIdAndQueryIgnoreCase(userId, "spring boot"))
                .thenReturn(Optional.of(existing));

        userService.recordSearch(userId, "spring boot");

        verify(searchHistoryRepository, times(1)).save(existing);
    }

    // ---- Browsing history ----

    @Test
    void recordBookView_newBook_createsEntry() {
        UUID bookId = UUID.randomUUID();
        when(browsingHistoryRepository.findByUserIdAndBookId(userId, bookId)).thenReturn(Optional.empty());

        userService.recordBookView(userId, bookId);

        verify(browsingHistoryRepository).save(any(BrowsingHistoryItem.class));
    }

    // ---- getMe / updateProfile (lazy provisioning) ----

    @Test
    void getMe_noExistingProfileOrWallet_provisionsBothWithSignupBonus() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletAccountRepository.findById(userId)).thenReturn(Optional.empty());
        when(walletAccountRepository.save(any(WalletAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        MeResponse response = userService.getMe(userId, "reader@example.com");

        assertThat(response.wallet().balance()).isEqualByComparingTo("500.00");
        verify(walletTransactionRepository).save(any());
    }

    @Test
    void getMe_existingProfileAndWallet_doesNotReprovision() {
        UserProfile profile = new UserProfile(userId);
        WalletAccount wallet = new WalletAccount(userId);
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(walletAccountRepository.findById(userId)).thenReturn(Optional.of(wallet));

        userService.getMe(userId, "reader@example.com");

        verify(userProfileRepository, never()).save(any());
        verify(walletAccountRepository, never()).save(any());
    }

    @Test
    void updateProfile_onlyAppliesFieldsThatAreNonNull() {
        UserProfile profile = new UserProfile(userId);
        profile.setDisplayName("Old Name");
        profile.setPhone("111");
        WalletAccount wallet = new WalletAccount(userId);
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(walletAccountRepository.findById(userId)).thenReturn(Optional.of(wallet));

        userService.updateProfile(userId, "reader@example.com",
                new UpdateProfileRequest("New Name", null, null, null));

        assertThat(profile.getDisplayName()).isEqualTo("New Name");
        assertThat(profile.getPhone()).isEqualTo("111");
    }

    @Test
    void updateProfile_favoriteCategoryIds_joinedAsCsv() {
        UserProfile profile = new UserProfile(userId);
        WalletAccount wallet = new WalletAccount(userId);
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(walletAccountRepository.findById(userId)).thenReturn(Optional.of(wallet));
        UUID cat1 = UUID.randomUUID();
        UUID cat2 = UUID.randomUUID();

        MeResponse response = userService.updateProfile(userId, "reader@example.com",
                new UpdateProfileRequest(null, null, null, List.of(cat1, cat2)));

        assertThat(response.favoriteCategoryIds()).containsExactlyInAnyOrder(cat1, cat2);
    }

    @Test
    void getAdminStoreId_noProfile_returnsNull() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(userService.getAdminStoreId(userId)).isNull();
    }

    @Test
    void getDisplayName_noProfile_returnsNullWithoutProvisioning() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(userService.getDisplayName(userId)).isNull();
        verify(userProfileRepository, never()).save(any());
    }

    // ---- Addresses ----

    @Test
    void addAddress_atLimit_throws() {
        when(addressRepository.countByUserIdAndDeletedAtIsNull(userId)).thenReturn(20L);

        assertThatThrownBy(() -> userService.addAddress(userId, addressRequest(false)))
                .isInstanceOf(AddressLimitReachedException.class);

        verify(addressRepository, never()).save(any());
    }

    @Test
    void addAddress_firstAddress_isForcedDefaultRegardlessOfRequest() {
        when(addressRepository.countByUserIdAndDeletedAtIsNull(userId)).thenReturn(0L);
        when(addressRepository.findAllByUserIdAndDeletedAtIsNullOrderByIsDefaultDesc(userId)).thenReturn(List.of());

        CreateAddressResponse response = userService.addAddress(userId, addressRequest(false));

        assertThat(response.isDefault()).isTrue();
    }

    @Test
    void addAddress_explicitDefault_clearsPreviousDefault() {
        when(addressRepository.countByUserIdAndDeletedAtIsNull(userId)).thenReturn(1L);
        Address existingDefault = new Address(userId, AddressLabel.HOME, AddressRecipientType.OWNER, "A", "L1", null,
                "City", "State", "000000", "IN", null, "123", true);
        when(addressRepository.findAllByUserIdAndDeletedAtIsNullOrderByIsDefaultDesc(userId))
                .thenReturn(List.of(existingDefault));

        userService.addAddress(userId, addressRequest(true));

        assertThat(existingDefault.isDefault()).isFalse();
        verify(addressRepository).save(existingDefault);
    }

    @Test
    void setDefaultAddress_notFound_throws() {
        UUID addressId = UUID.randomUUID();
        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.setDefaultAddress(userId, addressId))
                .isInstanceOf(AddressNotFoundException.class);
    }

    @Test
    void setDefaultAddress_alreadyDefault_isANoOp() {
        Address address = new Address(userId, AddressLabel.HOME, AddressRecipientType.OWNER, "A", "L1", null,
                "City", "State", "000000", "IN", null, "123", true);
        when(addressRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(address));

        userService.setDefaultAddress(userId, UUID.randomUUID());

        verify(addressRepository, never()).findAllByUserIdAndDeletedAtIsNullOrderByIsDefaultDesc(any());
    }

    @Test
    void deleteAddress_notFound_throws() {
        UUID addressId = UUID.randomUUID();
        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteAddress(userId, addressId))
                .isInstanceOf(AddressNotFoundException.class);
    }

    @Test
    void deleteAddress_found_softDeletesRatherThanRemoving() {
        Address address = new Address(userId, AddressLabel.HOME, AddressRecipientType.OWNER, "A", "L1", null,
                "City", "State", "000000", "IN", null, "123", false);
        when(addressRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(address));

        userService.deleteAddress(userId, UUID.randomUUID());

        verify(addressRepository).save(address);
        verify(addressRepository, never()).delete(any());
    }

    // ---- Wallet ----

    @Test
    void getWallet_pagesTransactionsAndReturnsCurrentBalance() {
        WalletAccount wallet = new WalletAccount(userId);
        wallet.credit(new BigDecimal("42.00"));
        when(walletAccountRepository.findById(userId)).thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.findAllByUserIdOrderByCreatedAtDesc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var response = userService.getWallet(userId, Pageable.unpaged());

        assertThat(response.balance()).isEqualByComparingTo("42.00");
    }

    @Test
    void topUp_creditsWalletAndRecordsTransaction() {
        WalletAccount wallet = new WalletAccount(userId);
        when(walletAccountRepository.findById(userId)).thenReturn(Optional.of(wallet));

        WalletBalanceResponse response = userService.topUp(userId, new BigDecimal("100.00"));

        assertThat(response.balance()).isEqualByComparingTo("100.00");
        verify(walletTransactionRepository).save(any());
    }

    @Test
    void redeemCoupon_unknownCode_throws() {
        when(couponRepository.findByCodeIgnoreCase("BADCODE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.redeemCoupon(userId, "BADCODE"))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    void redeemCoupon_expiredOrInactive_throwsNotRedeemable() throws Exception {
        Coupon coupon = newCoupon("EXPIRED", new BigDecimal("50.00"), false);
        when(couponRepository.findByCodeIgnoreCase("EXPIRED")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> userService.redeemCoupon(userId, "EXPIRED"))
                .isInstanceOf(CouponNotRedeemableException.class);
    }

    @Test
    void redeemCoupon_alreadyRedeemedByThisUser_throws() throws Exception {
        Coupon coupon = newCoupon("WELCOME10", new BigDecimal("50.00"), true);
        when(couponRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(Optional.of(coupon));
        when(couponRedemptionRepository.existsByCouponIdAndUserId(coupon.getId(), userId)).thenReturn(true);

        assertThatThrownBy(() -> userService.redeemCoupon(userId, "WELCOME10"))
                .isInstanceOf(CouponAlreadyRedeemedException.class);
    }

    @Test
    void redeemCoupon_valid_creditsWalletAndRecordsRedemption() throws Exception {
        Coupon coupon = newCoupon("WELCOME10", new BigDecimal("50.00"), true);
        when(couponRepository.findByCodeIgnoreCase("welcome10")).thenReturn(Optional.of(coupon));
        when(couponRedemptionRepository.existsByCouponIdAndUserId(coupon.getId(), userId)).thenReturn(false);
        WalletAccount wallet = new WalletAccount(userId);
        when(walletAccountRepository.findById(userId)).thenReturn(Optional.of(wallet));

        RedeemCouponResponse response = userService.redeemCoupon(userId, "  welcome10  ");

        assertThat(response.creditedAmount()).isEqualByComparingTo("50.00");
        verify(couponRedemptionRepository).save(any(CouponRedemption.class));
    }

    private static CreateAddressRequest addressRequest(boolean isDefault) {
        return new CreateAddressRequest(
                AddressLabel.HOME, AddressRecipientType.OWNER, "A Name", "9999999999",
                "Line 1", null, "City", "State", "000000", "IN", null, isDefault
        );
    }

    private static Coupon newCoupon(String code, BigDecimal amount, boolean active) throws Exception {
        Coupon coupon = new Coupon() { };
        setField(coupon, "id", UUID.randomUUID());
        setField(coupon, "code", code);
        setField(coupon, "amount", amount);
        setField(coupon, "active", active);
        return coupon;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = Coupon.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
