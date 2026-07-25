package com.readora.catalog.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when no category exists with the given id.
public class CategoryNotFoundException extends ServiceException {
    public CategoryNotFoundException() {
        super("CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND, "No such category");
    }
}
