package com.readora.user.service;

import com.readora.user.dto.AddressResponse;
import com.readora.user.dto.CreateAddressRequest;
import com.readora.user.dto.CreateAddressResponse;
import com.readora.user.dto.MeResponse;
import com.readora.user.dto.WalletResponse;
import com.readora.user.entity.Address;
import com.readora.user.entity.UserProfile;
import com.readora.user.entity.WalletAccount;
import com.readora.user.entity.WalletTransaction;
import com.readora.user.entity.WalletTransactionType;
import com.readora.user.exception.AddressLimitReachedException;
import com.readora.user.exception.AddressNotFoundException;
import com.readora.user.repository.AddressRepository;
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
    private final BigDecimal signupBonus;

    public UserService(
            UserProfileRepository userProfileRepository,
            AddressRepository addressRepository,
            WalletAccountRepository walletAccountRepository,
            WalletTransactionRepository walletTransactionRepository,
            @Value("${app.wallet.signup-bonus}") BigDecimal signupBonus
    ) {
        this.userProfileRepository = userProfileRepository;
        this.addressRepository = addressRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.signupBonus = signupBonus;
    }

    /**
     * There's no event in this system telling user-service a new account was created (the doc's
     * own Eventing topic list has none), so the profile + wallet are provisioned lazily on first
     * access instead — idempotent, and the 100.00 signup bonus lands the first time this runs
     * rather than at registration time.
     */
    @Transactional
    public MeResponse getMe(UUID userId, String email) {
        UserProfile profile = userProfileRepository.findById(userId).orElseGet(() -> provisionProfile(userId));
        WalletAccount wallet = walletAccountRepository.findById(userId).orElseGet(() -> provisionWallet(userId));

        return new MeResponse(
                userId, email, profile.getDisplayName(), profile.getAvatarUrl(), profile.getLocale(),
                new MeResponse.WalletSummary(wallet.getBalance(), wallet.getCurrency())
        );
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
        if (addressRepository.countByUserIdAndDeletedAtIsNull(userId) >= MAX_ADDRESSES) {
            throw new AddressLimitReachedException();
        }

        Address address = new Address(
                userId, request.label(), request.recipientName(), request.line1(), request.line2(),
                request.city(), request.state(), request.postalCode(), request.countryCode(),
                request.phone(), request.isDefault()
        );

        addressRepository.save(address);
        return new CreateAddressResponse(address.getId(), address.isDefault());
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

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(), address.getLabel(), address.getRecipientName(), address.getLine1(),
                address.getLine2(), address.getCity(), address.getState(), address.getPostalCode(),
                address.getCountryCode(), address.isDefault()
        );
    }
}
