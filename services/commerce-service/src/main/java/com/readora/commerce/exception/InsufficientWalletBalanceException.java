package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/** Thrown when a WALLET checkout's grand total exceeds the caller's current wallet balance. */
public class InsufficientWalletBalanceException extends ServiceException {

    public InsufficientWalletBalanceException(BigDecimal shortfall, String currency) {
        super(
                "INSUFFICIENT_WALLET_BALANCE",
                HttpStatus.PAYMENT_REQUIRED,
                "Your wallet balance is short by " + currency + " " + shortfall + ". Top up your wallet to continue."
        );
    }
}
