package com.readora.sharedcore.exception;

import com.readora.sharedcore.dto.ErrorResponse;
import com.readora.sharedcore.dto.FieldErrorItem;
import com.readora.sharedcore.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(ServiceException ex, HttpServletRequest request) {
        String traceId = traceId();
        log.debug("{} {} -> {} {} [{}]", request.getMethod(), request.getRequestURI(), ex.getStatus().value(), ex.getErrorCode(), traceId);

        ErrorResponse body = new ErrorResponse(
                ex.getErrorCode(), ex.getMessage(), ex.getStatus().value(),
                request.getRequestURI(), traceId, Instant.now()
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldErrorItem> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorItem(fe.getField(), fe.getDefaultMessage()))
                .toList();

        String traceId = traceId();
        log.debug("{} {} -> 400 VALIDATION_FAILED {} [{}]", request.getMethod(), request.getRequestURI(), fieldErrors, traceId);

        ErrorResponse body = new ErrorResponse(
                "VALIDATION_FAILED", "Request contains invalid fields.", HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(), traceId, Instant.now(), fieldErrors
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        String traceId = traceId();
        log.error("Unhandled exception on {} {} [{}]", request.getMethod(), request.getRequestURI(), traceId, ex);

        ErrorResponse body = new ErrorResponse(
                "INTERNAL_ERROR", "An unexpected error occurred.", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI(), traceId, Instant.now()
        );
        return ResponseEntity.internalServerError().body(body);
    }

    private String traceId() {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        return correlationId != null ? correlationId : UUID.randomUUID().toString().replace("-", "");
    }
}
