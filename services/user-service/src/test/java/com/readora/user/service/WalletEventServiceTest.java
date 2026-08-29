package com.readora.user.service;

import com.readora.user.dto.PaymentCapturedEvent;
import com.readora.user.dto.RefundCompletedEvent;
import com.readora.user.entity.WalletAccount;
import com.readora.user.entity.WalletTransaction;
import com.readora.user.entity.WalletTransactionType;
import com.readora.user.repository.WalletAccountRepository;
import com.readora.user.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletEventServiceTest {

    @Mock
    private WalletAccountRepository walletAccountRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    private WalletEventService walletEventService;

    private final UUID userId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        walletEventService = new WalletEventService(walletAccountRepository, walletTransactionRepository);
    }

    private void stubIdempotency(boolean debitProcessed, boolean cashbackProcessed) {
        when(walletTransactionRepository.existsByIdempotencyKey("payment:" + paymentId)).thenReturn(Boolean.valueOf(debitProcessed));
        when(walletTransactionRepository.existsByIdempotencyKey("cashback:" + paymentId)).thenReturn(Boolean.valueOf(cashbackProcessed));
    }

    @Test
    void handlePaymentCaptured_zeroWalletAmountAndZeroOrderAmount_isACompleteNoOp() {
        walletEventService.handlePaymentCaptured(new PaymentCapturedEvent(orderId, paymentId, userId, BigDecimal.ZERO, BigDecimal.ZERO));

        verify(walletAccountRepository, never()).findById(any());
        verify(walletTransactionRepository, never()).save(any());
    }

    @Test
    void handlePaymentCaptured_bothAlreadyProcessed_isFullyIdempotent() {
        stubIdempotency(true, true);

        walletEventService.handlePaymentCaptured(new PaymentCapturedEvent(orderId, paymentId, userId, new BigDecimal("500.00"), new BigDecimal("100.00")));

        verify(walletAccountRepository, never()).findById(any());
        verify(walletTransactionRepository, never()).save(any());
    }

    @Test
    void handlePaymentCaptured_zeroWalletAmountUsed_skipsDebitButStillCreditsCashback() {
        // walletAmountUsed is zero, so debitWalletIfUsedForPayment returns before ever checking
        // its own idempotency key — only cashback's key is actually consulted here.
        when(walletTransactionRepository.existsByIdempotencyKey("cashback:" + paymentId)).thenReturn(Boolean.valueOf(false));
        when(walletAccountRepository.findById(userId)).thenReturn(Optional.empty());
        when(walletAccountRepository.save(any(WalletAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 500 falls in the 500-1499 tier -> 2% cashback = 10.00.
        walletEventService.handlePaymentCaptured(new PaymentCapturedEvent(orderId, paymentId, userId, new BigDecimal("500.00"), BigDecimal.ZERO));

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(WalletTransactionType.CASHBACK);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    void handlePaymentCaptured_debitAndCashbackBothApply_bothRecordedIndependently() {
        WalletAccount wallet = new WalletAccount(userId);
        wallet.credit(new BigDecimal("300.00"));
        stubIdempotency(false, false);
        when(walletAccountRepository.findById(userId)).thenReturn(Optional.of(wallet));

        walletEventService.handlePaymentCaptured(new PaymentCapturedEvent(orderId, paymentId, userId, new BigDecimal("500.00"), new BigDecimal("100.00")));

        // 300 - 100 (debit) + 10 (2% cashback on the 500 order) = 210.
        assertThat(wallet.getBalance()).isEqualByComparingTo("210.00");

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository, times(2)).save(captor.capture());
        List<WalletTransaction> saved = captor.getAllValues();
        assertThat(saved).anySatisfy(tx -> {
            assertThat(tx.getType()).isEqualTo(WalletTransactionType.REDEEMED);
            assertThat(tx.getAmount()).isEqualByComparingTo("-100.00");
        });
        assertThat(saved).anySatisfy(tx -> {
            assertThat(tx.getType()).isEqualTo(WalletTransactionType.CASHBACK);
            assertThat(tx.getAmount()).isEqualByComparingTo("10.00");
        });
    }

    @Test
    void handlePaymentCaptured_debitAlreadyProcessed_cashbackStillAppliesIndependently() {
        stubIdempotency(true, false);
        WalletAccount wallet = new WalletAccount(userId);
        when(walletAccountRepository.findById(userId)).thenReturn(Optional.of(wallet));

        walletEventService.handlePaymentCaptured(new PaymentCapturedEvent(orderId, paymentId, userId, new BigDecimal("500.00"), new BigDecimal("100.00")));

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(WalletTransactionType.CASHBACK);
    }

    @Test
    void handlePaymentCaptured_noWalletYet_lazilyCreatesOneForCashback() {
        when(walletTransactionRepository.existsByIdempotencyKey("cashback:" + paymentId)).thenReturn(Boolean.valueOf(false));
        when(walletAccountRepository.findById(userId)).thenReturn(Optional.empty());
        when(walletAccountRepository.save(any(WalletAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletEventService.handlePaymentCaptured(new PaymentCapturedEvent(orderId, paymentId, userId, new BigDecimal("500.00"), BigDecimal.ZERO));

        // Once to lazily create the account, once to persist the cashback credit.
        verify(walletAccountRepository, times(2)).save(any(WalletAccount.class));
    }

    @Test
    void calculateCashback_appliesTheCorrectTierForEachOrderValue() {
        assertThat(WalletEventService.calculateCashback(new BigDecimal("100"))).isEqualByComparingTo("1.00");
        assertThat(WalletEventService.calculateCashback(new BigDecimal("500"))).isEqualByComparingTo("10.00");
        assertThat(WalletEventService.calculateCashback(new BigDecimal("1500"))).isEqualByComparingTo("45.00");
        assertThat(WalletEventService.calculateCashback(new BigDecimal("3000"))).isEqualByComparingTo("120.00");
        assertThat(WalletEventService.calculateCashback(new BigDecimal("5000"))).isEqualByComparingTo("250.00");
    }

    @Test
    void calculateCashback_nonPositiveOrNullAmount_returnsZero() {
        assertThat(WalletEventService.calculateCashback(BigDecimal.ZERO)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(WalletEventService.calculateCashback(new BigDecimal("-50"))).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(WalletEventService.calculateCashback(null)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void handleRefundCompleted_creditsWalletAndRecordsReversedTransaction() {
        UUID refundId = UUID.randomUUID();
        WalletAccount wallet = new WalletAccount(userId);
        when(walletTransactionRepository.existsByIdempotencyKey("refund:" + refundId)).thenReturn(Boolean.valueOf(false));
        when(walletAccountRepository.findById(userId)).thenReturn(Optional.of(wallet));

        walletEventService.handleRefundCompleted(new RefundCompletedEvent(orderId, refundId, userId, new BigDecimal("100.00"), new BigDecimal("100.00")));

        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(WalletTransactionType.REVERSED);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void handleRefundCompleted_alreadyProcessed_isIdempotent() {
        UUID refundId = UUID.randomUUID();
        when(walletTransactionRepository.existsByIdempotencyKey("refund:" + refundId)).thenReturn(Boolean.valueOf(true));

        walletEventService.handleRefundCompleted(new RefundCompletedEvent(orderId, refundId, userId, new BigDecimal("100.00"), new BigDecimal("100.00")));

        verify(walletAccountRepository, never()).findById(any());
    }
}
