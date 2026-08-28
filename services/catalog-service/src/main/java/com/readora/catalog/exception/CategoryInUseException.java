package com.readora.catalog.exception;

import org.springframework.http.HttpStatus;

public class CategoryInUseException extends ServiceException {
    public CategoryInUseException() {
        super("CATEGORY_IN_USE", HttpStatus.CONFLICT, "This category is assigned to one or more books — reassign them first");
    }
}
