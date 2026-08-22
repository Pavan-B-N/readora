package com.readora.payment.exception;

import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends ServiceException {
    public PaymentNotFoundException() {
        super("PAYMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "No payment recorded for that order yet");
    }
}
