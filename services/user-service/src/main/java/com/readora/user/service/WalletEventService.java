package com.readora.user.service;

import com.readora.sharedcore.event.PaymentCapturedEvent;
import com.readora.sharedcore.event.RefundCompletedEvent;
import com.readora.user.entity.WalletAccount;
import com.readora.user.entity.WalletTransaction;
import com.readora.user.entity.WalletTransactionType;
import com.readora.user.repository.WalletAccountRepository;
import com.readora.user.repository.WalletTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

// Reacts to payment/refund events to keep the wallet ledger consistent with money moved elsewhere; walletAmountUsed is always zero for now since commerce-service doesn't yet support wallet-funded checkout, but this listener is already wired up to start debiting once that lands.
@Service
public class WalletEventService {

    private static final Logger log = LoggerFactory.getLogger(WalletEventService.class);

    /** Order value at/above which a tier's cashback rate applies — checked highest-first. */
    private static final BigDecimal[] CASHBACK_TIER_FLOORS = {
            new BigDecimal("5000"), new BigDecimal("3000"), new BigDecimal("1500"), new BigDecimal("500"), BigDecimal.ZERO
    };
    private static final BigDecimal[] CASHBACK_TIER_RATES = {
            new BigDecimal("0.05"), new BigDecimal("0.04"), new BigDecimal("0.03"), new BigDecimal("0.02"), new BigDecimal("0.01")
    };

    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    // Wires in the wallet account and transaction repositories.
    public WalletEventService(
            WalletAccountRepository walletAccountRepository,
            WalletTransactionRepository walletTransactionRepository
    ) {
        this.walletAccountRepository = walletAccountRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    /** Wallet debit (if wallet funded part of the order) and cashback credit are independent — both, either, or neither can apply to a given order. */
    @Transactional
    public void handlePaymentCaptured(PaymentCapturedEvent event) {
        debitWalletIfUsedForPayment(event);
        creditCashback(event);
    }

    // Debits the wallet for the portion of the order it funded, if any, idempotently by paymentId.
    private void debitWalletIfUsedForPayment(PaymentCapturedEvent event) {
        BigDecimal walletAmountUsed = event.walletAmountUsed();
        if (walletAmountUsed == null || walletAmountUsed.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String idempotencyKey = "payment:" + event.paymentId();
        if (walletTransactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        WalletAccount wallet = walletAccountRepository.findById(event.userId())
                .orElseGet(() -> walletAccountRepository.save(new WalletAccount(event.userId())));

        wallet.debit(walletAmountUsed);
        walletAccountRepository.save(wallet);

        walletTransactionRepository.save(new WalletTransaction(
                event.userId(), event.orderId(), walletAmountUsed.negate(), WalletTransactionType.REDEEMED,
                wallet.getBalance(), idempotencyKey
        ));

        log.info("Debited {} from wallet for user {} (order {}, new balance {})",
                walletAmountUsed, event.userId(), event.orderId(), wallet.getBalance());
    }

    // Every captured payment earns cashback on the order's full amount, recorded under a separate "cashback:" idempotency namespace from the "payment:" debit above so both can share the same paymentId.
    private void creditCashback(PaymentCapturedEvent event) {
        String idempotencyKey = "cashback:" + event.paymentId();
        if (walletTransactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        BigDecimal cashbackAmount = calculateCashback(event.amount());
        if (cashbackAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        WalletAccount wallet = walletAccountRepository.findById(event.userId())
                .orElseGet(() -> walletAccountRepository.save(new WalletAccount(event.userId())));

        wallet.credit(cashbackAmount);
        walletAccountRepository.save(wallet);

        walletTransactionRepository.save(new WalletTransaction(
                event.userId(), event.orderId(), cashbackAmount, WalletTransactionType.CASHBACK,
                wallet.getBalance(), idempotencyKey
        ));

        log.info("Credited {} cashback to wallet for user {} (order {}, new balance {})",
                cashbackAmount, event.userId(), event.orderId(), wallet.getBalance());
    }

    /** Tiered 1%-5% cashback — bigger baskets earn a higher rate. Package-private so tests can hit it directly. */
    static BigDecimal calculateCashback(BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return orderAmount.multiply(cashbackRate(orderAmount)).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    // Looks up the applicable cashback rate for the order's amount from the tier tables.
    private static BigDecimal cashbackRate(BigDecimal orderAmount) {
        for (int i = 0; i < CASHBACK_TIER_FLOORS.length; i++) {
            if (orderAmount.compareTo(CASHBACK_TIER_FLOORS[i]) >= 0) {
                return CASHBACK_TIER_RATES[i];
            }
        }
        return CASHBACK_TIER_RATES[CASHBACK_TIER_RATES.length - 1];
    }

    // Credits back the wallet-funded portion of an order once its refund completes, idempotently by refundId.
    @Transactional
    public void handleRefundCompleted(RefundCompletedEvent event) {
        BigDecimal amountToReverse = event.walletAmountToReverse();
        if (amountToReverse == null || amountToReverse.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String idempotencyKey = "refund:" + event.refundId();
        if (walletTransactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        WalletAccount wallet = walletAccountRepository.findById(event.userId())
                .orElseGet(() -> walletAccountRepository.save(new WalletAccount(event.userId())));

        wallet.credit(amountToReverse);
        walletAccountRepository.save(wallet);

        walletTransactionRepository.save(new WalletTransaction(
                event.userId(), event.orderId(), amountToReverse, WalletTransactionType.REVERSED,
                wallet.getBalance(), idempotencyKey
        ));

        log.info("Reversed {} to wallet for user {} (order {}, refund {}, new balance {})",
                amountToReverse, event.userId(), event.orderId(), event.refundId(), wallet.getBalance());
    }
}
