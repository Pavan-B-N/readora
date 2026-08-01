package com.readora.payment.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when no payment has been recorded yet for the requested order.
public class PaymentNotFoundException extends ServiceException {
    // Creates the exception with a fixed message.
    public PaymentNotFoundException() {
        super("PAYMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "No payment recorded for that order yet");
    }
}
