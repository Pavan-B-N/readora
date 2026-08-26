package com.readora.user.service;

import com.readora.user.dto.AddressResponse;
import com.readora.user.dto.CreateAddressRequest;
import com.readora.user.dto.CreateAddressResponse;
import com.readora.user.dto.MeResponse;
import com.readora.user.dto.RedeemCouponResponse;
import com.readora.user.dto.UpdateProfileRequest;
import com.readora.user.dto.WalletBalanceResponse;
import com.readora.user.dto.WalletResponse;
import com.readora.user.entity.Address;
import com.readora.user.entity.Coupon;
import com.readora.user.entity.CouponRedemption;
import com.readora.user.entity.UserProfile;
import com.readora.user.entity.WalletAccount;
import com.readora.user.entity.WalletTransaction;
import com.readora.user.entity.WalletTransactionType;
import com.readora.user.exception.AddressLimitReachedException;
import com.readora.user.exception.AddressNotFoundException;
import com.readora.user.exception.CouponAlreadyRedeemedException;
import com.readora.user.exception.CouponNotFoundException;
import com.readora.user.exception.CouponNotRedeemableException;
import com.readora.user.repository.AddressRepository;
import com.readora.user.repository.CouponRedemptionRepository;
import com.readora.user.repository.CouponRepository;
import com.readora.user.repository.UserProfileRepository;
import com.readora.user.repository.WalletAccountRepository;
import com.readora.user.repository.WalletTransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private static final int MAX_ADDRESSES = 20;

    private final UserProfileRepository userProfileRepository;
    private final AddressRepository addressRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final BigDecimal signupBonus;

    public UserService(
            UserProfileRepository userProfileRepository,
            AddressRepository addressRepository,
            WalletAccountRepository walletAccountRepository,
            WalletTransactionRepository walletTransactionRepository,
            CouponRepository couponRepository,
            CouponRedemptionRepository couponRedemptionRepository,
            @Value("${app.wallet.signup-bonus}") BigDecimal signupBonus
    ) {
        this.userProfileRepository = userProfileRepository;
        this.addressRepository = addressRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.couponRepository = couponRepository;
        this.couponRedemptionRepository = couponRedemptionRepository;
        this.signupBonus = signupBonus;
    }

    /**
     * There's no event in this system telling user-service a new account was created (the doc's
     * own Eventing topic list has none), so the profile + wallet are provisioned lazily on first
     * access instead — idempotent, and the signup bonus lands the first time this runs rather
     * than at registration time.
     */
    @Transactional
    public MeResponse getMe(UUID userId, String email) {
        UserProfile profile = userProfileRepository.findById(userId).orElseGet(() -> provisionProfile(userId));
        WalletAccount wallet = walletAccountRepository.findById(userId).orElseGet(() -> provisionWallet(userId));
        return toMeResponse(userId, email, profile, wallet);
    }

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

    /**
     * Called only by catalog-service (via the internal, gateway-secret-gated endpoint) to resolve
     * which store an admin's book-management requests are scoped to. Never derived from — or
     * settable through — {@link #updateProfile}: that's the whole point, see
     * {@link UserProfile#getAdminStoreId()}.
     */
    @Transactional(readOnly = true)
    public UUID getAdminStoreId(UUID userId) {
        return userProfileRepository.findById(userId).map(UserProfile::getAdminStoreId).orElse(null);
    }

    /** Read-only lookup — unlike getMe(), does not provision a profile as a side effect. */
    @Transactional(readOnly = true)
    public String getDisplayName(UUID userId) {
        return userProfileRepository.findById(userId).map(UserProfile::getDisplayName).orElse(null);
    }

    private UserProfile provisionProfile(UUID userId) {
        return userProfileRepository.save(new UserProfile(userId));
    }

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

    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(UUID userId) {
        return addressRepository.findAllByUserIdAndDeletedAtIsNullOrderByIsDefaultDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CreateAddressResponse addAddress(UUID userId, CreateAddressRequest request) {
        long existingCount = addressRepository.countByUserIdAndDeletedAtIsNull(userId);
        if (existingCount >= MAX_ADDRESSES) {
            throw new AddressLimitReachedException();
        }

        // The first address is always the default — and any explicit request to make one
        // default must actually clear the others, or two rows end up isDefault=true at once.
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

    private void clearExistingDefault(UUID userId) {
        addressRepository.findAllByUserIdAndDeletedAtIsNullOrderByIsDefaultDesc(userId).stream()
                .filter(Address::isDefault)
                .forEach(existing -> {
                    existing.clearDefault();
                    addressRepository.save(existing);
                });
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(AddressNotFoundException::new);
        address.softDelete();
        addressRepository.save(address);
    }

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

    /**
     * Dummy top-up — there's no real payment gateway in this build (project scope, see
     * PaymentService), so this credits the wallet directly rather than collecting real money.
     * Exists so checkout's "insufficient balance" path has somewhere to send the user.
     */
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

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(), address.getLabel(), address.getRecipientType(), address.getRecipientName(),
                address.getPhone(), address.getLine1(), address.getLine2(), address.getCity(), address.getState(),
                address.getPostalCode(), address.getCountryCode(), address.getStoreId(), address.isDefault()
        );
    }
}
