package com.readora.ai.exception;

import org.springframework.http.HttpStatus;

public class BookNotIndexedException extends ServiceException {
    public BookNotIndexedException() {
        super("BOOK_NOT_INDEXED", HttpStatus.CONFLICT, "This book hasn't been prepared for the assistant yet — initialize it first");
    }
}
