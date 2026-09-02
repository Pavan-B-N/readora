package com.readora.ai.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class BookAccessDeniedException extends ServiceException {
    public BookAccessDeniedException() {
        super("BOOK_ACCESS_DENIED", HttpStatus.FORBIDDEN, "You haven't purchased this book");
    }
}
