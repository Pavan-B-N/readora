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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @Test
    void handlePaymentCaptured_zeroWalletAmount_isANoOp() {
        walletEventService.handlePaymentCaptured(new PaymentCapturedEvent(orderId, paymentId, userId, new BigDecimal("500.00"), BigDecimal.ZERO));

        verify(walletAccountRepository, never()).findById(any());
        verify(walletTransactionRepository, never()).save(any());
    }

    @Test
    void handlePaymentCaptured_alreadyProcessed_isIdempotent() {
        when(walletTransactionRepository.existsByIdempotencyKey("payment:" + paymentId)).thenReturn(Boolean.valueOf(true));

        walletEventService.handlePaymentCaptured(new PaymentCapturedEvent(orderId, paymentId, userId, new BigDecimal("500.00"), new BigDecimal("100.00")));

        verify(walletAccountRepository, never()).findById(any());
        verify(walletTransactionRepository, never()).save(any());
    }

    @Test
    void handlePaymentCaptured_debitsWalletAndRecordsNegativeRedeemedTransaction() {
        WalletAccount wallet = new WalletAccount(userId);
        wallet.credit(new BigDecimal("300.00"));
        when(walletTransactionRepository.existsByIdempotencyKey(any())).thenReturn(Boolean.valueOf(false));
        when(walletAccountRepository.findById(userId)).thenReturn(Optional.of(wallet));

        walletEventService.handlePaymentCaptured(new PaymentCapturedEvent(orderId, paymentId, userId, new BigDecimal("500.00"), new BigDecimal("100.00")));

        assertThat(wallet.getBalance()).isEqualByComparingTo("200.00");

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(WalletTransactionType.REDEEMED);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("-100.00");
    }

    @Test
    void handlePaymentCaptured_noExistingWallet_createsOneBeforeDebiting() {
        when(walletTransactionRepository.existsByIdempotencyKey(any())).thenReturn(Boolean.valueOf(false));
        when(walletAccountRepository.findById(userId)).thenReturn(Optional.empty());
        when(walletAccountRepository.save(any(WalletAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletEventService.handlePaymentCaptured(new PaymentCapturedEvent(orderId, paymentId, userId, new BigDecimal("500.00"), new BigDecimal("50.00")));

        verify(walletAccountRepository, org.mockito.Mockito.times(2)).save(any(WalletAccount.class));
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
