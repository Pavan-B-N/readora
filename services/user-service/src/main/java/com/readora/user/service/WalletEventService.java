package com.readora.user.service;

import com.readora.user.dto.PaymentCapturedEvent;
import com.readora.user.dto.RefundCompletedEvent;
import com.readora.user.entity.WalletAccount;
import com.readora.user.entity.WalletTransaction;
import com.readora.user.entity.WalletTransactionType;
import com.readora.user.repository.WalletAccountRepository;
import com.readora.user.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Reacts to payment/refund events to keep the wallet ledger consistent with money actually
 * moved elsewhere. commerce-service doesn't yet support wallet-funded checkout in this build
 * (deferred, see batch summary), so walletAmountUsed is always zero for now — this listener is
 * still wired up correctly and will just start debiting once that's added, no changes needed here.
 */
@Service
public class WalletEventService {

    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletEventService(
            WalletAccountRepository walletAccountRepository,
            WalletTransactionRepository walletTransactionRepository
    ) {
        this.walletAccountRepository = walletAccountRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Transactional
    public void handlePaymentCaptured(PaymentCapturedEvent event) {
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
    }

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
    }
}
