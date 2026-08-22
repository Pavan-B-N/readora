package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

public class QtyLimitExceededException extends ServiceException {
    public QtyLimitExceededException() {
        super("QTY_LIMIT_EXCEEDED", HttpStatus.UNPROCESSABLE_ENTITY, "More than 10 of a single title");
    }
}
