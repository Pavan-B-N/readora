package com.readora.catalog.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class ReviewNotFoundException extends ServiceException {
    public ReviewNotFoundException() {
        super("REVIEW_NOT_FOUND", HttpStatus.NOT_FOUND, "No such review");
    }
}
