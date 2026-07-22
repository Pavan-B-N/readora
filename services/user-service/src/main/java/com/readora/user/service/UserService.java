package com.readora.user.service;

import com.readora.user.dto.AddressResponse;
import com.readora.user.dto.BrowsingHistoryItemResponse;
import com.readora.user.dto.CreateAddressRequest;
import com.readora.user.dto.CreateAddressResponse;
import com.readora.user.dto.MeResponse;
import com.readora.user.dto.RedeemCouponResponse;
import com.readora.user.dto.SearchHistoryItemResponse;
import com.readora.user.dto.UpdateProfileRequest;
import com.readora.user.dto.WalletBalanceResponse;
import com.readora.user.dto.WalletResponse;
import com.readora.user.dto.WishlistItemResponse;
import com.readora.user.entity.Address;
import com.readora.user.entity.BrowsingHistoryItem;
import com.readora.user.entity.Coupon;
import com.readora.user.entity.CouponRedemption;
import com.readora.user.entity.SearchHistoryItem;
import com.readora.user.entity.UserProfile;
import com.readora.user.entity.WalletAccount;
import com.readora.user.entity.WalletTransaction;
import com.readora.user.entity.WalletTransactionType;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Core business logic for profiles, addresses, wallet, coupons, wishlist, and browsing/search history.
@Service
public class UserService {

    private static final int MAX_ADDRESSES = 20;

    private final UserProfileRepository userProfileRepository;
    private final AddressRepository addressRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final WishlistRepository wishlistRepository;
    private final BrowsingHistoryRepository browsingHistoryRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final BigDecimal signupBonus;

    // Wires in the repositories and the configured signup bonus amount.
    public UserService(
            UserProfileRepository userProfileRepository,
            AddressRepository addressRepository,
            WalletAccountRepository walletAccountRepository,
            WalletTransactionRepository walletTransactionRepository,
            CouponRepository couponRepository,
            CouponRedemptionRepository couponRedemptionRepository,
            WishlistRepository wishlistRepository,
            BrowsingHistoryRepository browsingHistoryRepository,
            SearchHistoryRepository searchHistoryRepository,
            @Value("${app.wallet.signup-bonus}") BigDecimal signupBonus
    ) {
        this.userProfileRepository = userProfileRepository;
        this.addressRepository = addressRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.couponRepository = couponRepository;
        this.couponRedemptionRepository = couponRedemptionRepository;
        this.wishlistRepository = wishlistRepository;
        this.browsingHistoryRepository = browsingHistoryRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.signupBonus = signupBonus;
    }

    // Returns the user's wishlist, most recently added first.
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> listWishlist(UUID userId) {
        return wishlistRepository.findAllByUserIdOrderByAddedAtDesc(userId).stream()
                .map(item -> new WishlistItemResponse(item.getBookId(), item.getAddedAt()))
                .toList();
    }

    /** Idempotent — adding a book already on the wishlist is a no-op, not a conflict. */
    @Transactional
    public void addToWishlist(UUID userId, UUID bookId) {
        if (wishlistRepository.existsByUserIdAndBookId(userId, bookId)) {
            return;
        }
        wishlistRepository.save(new WishlistItem(userId, bookId));
    }

    /** Idempotent — removing a book that isn't on the wishlist is a no-op, not a 404. */
    @Transactional
    public void removeFromWishlist(UUID userId, UUID bookId) {
        wishlistRepository.findByUserIdAndBookId(userId, bookId).ifPresent(wishlistRepository::delete);
    }

    // Returns the user's most recently viewed books, capped at 20.
    @Transactional(readOnly = true)
    public List<BrowsingHistoryItemResponse> listBrowsingHistory(UUID userId) {
        return browsingHistoryRepository.findTop20ByUserIdOrderByViewedAtDesc(userId).stream()
                .map(item -> new BrowsingHistoryItemResponse(item.getBookId(), item.getViewedAt()))
                .toList();
    }

    /** Upsert — viewing a book already in history bumps it back to the top instead of duplicating it. */
    @Transactional
    public void recordBookView(UUID userId, UUID bookId) {
        BrowsingHistoryItem item = browsingHistoryRepository.findByUserIdAndBookId(userId, bookId)
                .orElseGet(() -> new BrowsingHistoryItem(userId, bookId));
        item.touch();
        browsingHistoryRepository.save(item);
    }

    // Returns the user's most recent search terms, capped at 20.
    @Transactional(readOnly = true)
    public List<SearchHistoryItemResponse> listSearchHistory(UUID userId) {
        return searchHistoryRepository.findTop20ByUserIdOrderBySearchedAtDesc(userId).stream()
                .map(item -> new SearchHistoryItemResponse(item.getQuery(), item.getSearchedAt()))
                .toList();
    }

    /** Upsert — re-searching the same term (case-insensitively) bumps it back to the top instead of duplicating it. */
    @Transactional
    public void recordSearch(UUID userId, String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        SearchHistoryItem item = searchHistoryRepository.findByUserIdAndQueryIgnoreCase(userId, trimmed)
                .orElseGet(() -> new SearchHistoryItem(userId, trimmed));
        item.touch();
        searchHistoryRepository.save(item);
    }

    /** Internal, best-effort read for catalog-service's recommendation engine — capped at the stored top-20. */
    @Transactional(readOnly = true)
    public List<UUID> getRecentBookViewIds(UUID userId, int limit) {
        return browsingHistoryRepository.findTop20ByUserIdOrderByViewedAtDesc(userId).stream()
                .map(BrowsingHistoryItem::getBookId)
                .limit(limit)
                .toList();
    }

    /** Internal, best-effort read for catalog-service's recommendation engine — capped at the stored top-20. */
    @Transactional(readOnly = true)
    public List<String> getRecentSearchTerms(UUID userId, int limit) {
        return searchHistoryRepository.findTop20ByUserIdOrderBySearchedAtDesc(userId).stream()
                .map(SearchHistoryItem::getQuery)
                .limit(limit)
                .toList();
    }

    // No account-created event exists in this system, so the profile + wallet are provisioned lazily here on first access — idempotent, and the signup bonus lands on this first run rather than at registration.
    @Transactional
    public MeResponse getMe(UUID userId, String email) {
        UserProfile profile = userProfileRepository.findById(userId).orElseGet(() -> provisionProfile(userId));
        WalletAccount wallet = walletAccountRepository.findById(userId).orElseGet(() -> provisionWallet(userId));
        return toMeResponse(userId, email, profile, wallet);
    }

    // Applies only the non-null fields of the request to the profile, provisioning it first if needed.
    @Transactional
    public MeResponse updateProfile(UUID userId, String email, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findById(userId).orElseGet(() -> provisionProfile(userId));
        WalletAccount wallet = walletAccountRepository.findById(userId).orElseGet(() -> provisionWallet(userId));

        if (request.displayName() != null) profile.setDisplayName(request.displayName());
        if (request.phone() != null) profile.setPhone(request.phone());
        if (request.preferredStoreId() != null) profile.setPreferredStoreId(request.preferredStoreId());
        if (request.favoriteCategoryIds() != null) {
            profile.setFavoriteCategoryIds(
                    request.favoriteCategoryIds().stream().map(UUID::toString).collect(java.util.stream.Collectors.joining(","))
            );
        }
        userProfileRepository.save(profile);

        return toMeResponse(userId, email, profile, wallet);
    }

    // Assembles the /me response, parsing the stored CSV of favorite category IDs back into a list.
    private MeResponse toMeResponse(UUID userId, String email, UserProfile profile, WalletAccount wallet) {
        List<UUID> favoriteCategoryIds = profile.getFavoriteCategoryIds() == null || profile.getFavoriteCategoryIds().isBlank()
                ? List.of()
                : List.of(profile.getFavoriteCategoryIds().split(",")).stream().map(UUID::fromString).toList();

        return new MeResponse(
                userId, email, profile.getDisplayName(), profile.getAvatarUrl(), profile.getPhone(), profile.getLocale(),
                profile.getPreferredStoreId(), profile.getAdminStoreId(), favoriteCategoryIds,
                new MeResponse.WalletSummary(wallet.getBalance(), wallet.getCurrency())
        );
    }

    // Called only by catalog-service (internal, gateway-secret-gated) to resolve which store an admin's book-management requests are scoped to; never derived from or settable through {@link #updateProfile} — see {@link UserProfile#getAdminStoreId()}.
    @Transactional(readOnly = true)
    public UUID getAdminStoreId(UUID userId) {
        return userProfileRepository.findById(userId).map(UserProfile::getAdminStoreId).orElse(null);
    }

    // The reverse of getAdminStoreId() — called by commerce-service to find who to notify on a return request or return-chat message; each store has exactly one admin, so "first" is also "only."
    @Transactional(readOnly = true)
    public UUID getAdminUserIdForStore(UUID storeId) {
        return userProfileRepository.findFirstByAdminStoreId(storeId).map(UserProfile::getUserId).orElse(null);
    }

    /** Read-only lookup — unlike getMe(), does not provision a profile as a side effect. */
    @Transactional(readOnly = true)
    public String getDisplayName(UUID userId) {
        return userProfileRepository.findById(userId).map(UserProfile::getDisplayName).orElse(null);
    }

    // Creates and saves a blank profile for a user seen for the first time.
    private UserProfile provisionProfile(UUID userId) {
        return userProfileRepository.save(new UserProfile(userId));
    }

    // Creates a wallet, credits the signup bonus, and records the bonus transaction.
    private WalletAccount provisionWallet(UUID userId) {
        WalletAccount wallet = new WalletAccount(userId);
        wallet.credit(signupBonus);
        walletAccountRepository.save(wallet);

        walletTransactionRepository.save(new WalletTransaction(
                userId, null, signupBonus, WalletTransactionType.SIGNUP_BONUS,
                wallet.getBalance(), "signup:" + userId
        ));

        return wallet;
    }

    // Returns the user's non-deleted addresses, default first.
    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(UUID userId) {
        return addressRepository.findAllByUserIdAndDeletedAtIsNullOrderByIsDefaultDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    // Creates a new address, enforcing the per-user cap and the single-default invariant.
    @Transactional
    public CreateAddressResponse addAddress(UUID userId, CreateAddressRequest request) {
        long existingCount = addressRepository.countByUserIdAndDeletedAtIsNull(userId);
        if (existingCount >= MAX_ADDRESSES) {
            throw new AddressLimitReachedException();
        }

        // The first address is always default, and an explicit default request must clear the others so two rows never end up isDefault=true at once.
        boolean makeDefault = request.isDefault() || existingCount == 0;
        if (makeDefault) {
            clearExistingDefault(userId);
        }

        Address address = new Address(
                userId, request.label(), request.recipientType(), request.recipientName(), request.line1(),
                request.line2(), request.city(), request.state(), request.postalCode(), request.countryCode(),
                request.storeId(), request.recipientPhone(), makeDefault
        );

        addressRepository.save(address);
        return new CreateAddressResponse(address.getId(), address.isDefault());
    }

    // Makes the given address the default, clearing any previous default first.
    @Transactional
    public void setDefaultAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(AddressNotFoundException::new);
        if (address.isDefault()) {
            return;
        }
        clearExistingDefault(userId);
        address.markDefault();
        addressRepository.save(address);
    }

    // Clears the isDefault flag on whichever address currently holds it, if any.
    private void clearExistingDefault(UUID userId) {
        addressRepository.findAllByUserIdAndDeletedAtIsNullOrderByIsDefaultDesc(userId).stream()
                .filter(Address::isDefault)
                .forEach(existing -> {
                    existing.clearDefault();
                    addressRepository.save(existing);
                });
    }

    // Soft-deletes an address owned by the user.
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(AddressNotFoundException::new);
        address.softDelete();
        addressRepository.save(address);
    }

    // Returns the wallet balance plus a page of its transaction history.
    @Transactional(readOnly = true)
    public WalletResponse getWallet(UUID userId, Pageable pageable) {
        WalletAccount wallet = walletAccountRepository.findById(userId).orElseGet(() -> provisionWallet(userId));
        Page<WalletTransaction> page = walletTransactionRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<WalletResponse.Item> items = page.getContent().stream()
                .map(tx -> new WalletResponse.Item(
                        tx.getId(), tx.getAmount(), tx.getType().name(), tx.getBalanceAfter(), tx.getOrderId(), tx.getCreatedAt()
                ))
                .toList();

        return new WalletResponse(wallet.getBalance(), wallet.getCurrency(), items);
    }

    /** Called synchronously by commerce-service at checkout — must reflect the current balance, not a cache. */
    @Transactional
    public WalletBalanceResponse getBalance(UUID userId) {
        WalletAccount wallet = walletAccountRepository.findById(userId).orElseGet(() -> provisionWallet(userId));
        return new WalletBalanceResponse(wallet.getBalance(), wallet.getCurrency());
    }

    // Dummy top-up — no real payment gateway exists in this build, so this credits the wallet directly rather than collecting real money; exists so checkout's "insufficient balance" path has somewhere to send the user.
    @Transactional
    public WalletBalanceResponse topUp(UUID userId, BigDecimal amount) {
        WalletAccount wallet = walletAccountRepository.findById(userId).orElseGet(() -> provisionWallet(userId));
        wallet.credit(amount);
        walletAccountRepository.save(wallet);

        walletTransactionRepository.save(new WalletTransaction(
                userId, null, amount, WalletTransactionType.TOPUP,
                wallet.getBalance(), "topup:" + UUID.randomUUID()
        ));

        return new WalletBalanceResponse(wallet.getBalance(), wallet.getCurrency());
    }

    /** Amazon-Pay-style: a code credits the wallet directly, once per user. */
    @Transactional
    public RedeemCouponResponse redeemCoupon(UUID userId, String code) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim()).orElseThrow(CouponNotFoundException::new);
        if (!coupon.isRedeemable()) {
            throw new CouponNotRedeemableException();
        }
        if (couponRedemptionRepository.existsByCouponIdAndUserId(coupon.getId(), userId)) {
            throw new CouponAlreadyRedeemedException();
        }

        WalletAccount wallet = walletAccountRepository.findById(userId).orElseGet(() -> provisionWallet(userId));
        wallet.credit(coupon.getAmount());
        walletAccountRepository.save(wallet);

        walletTransactionRepository.save(new WalletTransaction(
                userId, null, coupon.getAmount(), WalletTransactionType.COUPON_REDEEMED,
                wallet.getBalance(), "coupon:" + coupon.getId() + ":" + userId
        ));
        couponRedemptionRepository.save(new CouponRedemption(coupon.getId(), userId));

        return new RedeemCouponResponse(coupon.getAmount(), wallet.getBalance(), wallet.getCurrency());
    }

    // Maps an Address entity to its response DTO.
    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(), address.getLabel(), address.getRecipientType(), address.getRecipientName(),
                address.getPhone(), address.getLine1(), address.getLine2(), address.getCity(), address.getState(),
                address.getPostalCode(), address.getCountryCode(), address.getStoreId(), address.isDefault()
        );
    }
}
