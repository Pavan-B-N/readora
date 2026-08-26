package com.readora.notification.exception;

import com.readora.notification.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(ServiceException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                ex.getErrorCode(), ex.getMessage(), ex.getStatus().value(), request.getRequestURI(), null, Instant.now()
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}
