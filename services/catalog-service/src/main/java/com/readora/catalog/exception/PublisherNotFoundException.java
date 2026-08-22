package com.readora.catalog.exception;

import org.springframework.http.HttpStatus;

public class PublisherNotFoundException extends ServiceException {
    public PublisherNotFoundException() {
        super("PUBLISHER_NOT_FOUND", HttpStatus.NOT_FOUND, "No such publisher");
    }
}
