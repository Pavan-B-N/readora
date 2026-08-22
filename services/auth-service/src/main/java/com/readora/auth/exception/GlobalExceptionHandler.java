package com.readora.auth.exception;

import com.readora.auth.dto.ErrorResponse;
import com.readora.auth.dto.FieldErrorItem;
import com.readora.auth.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Maps every exception this service can throw into the shared {@link ErrorResponse} envelope. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles every domain exception ({@link AuthException} and its subclasses), using the
     * error code and status each one carries.
     *
     * @param ex      the domain exception that was thrown
     * @param request the current request, used to read the request path
     * @return an ErrorResponse body with the exception's status code
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getStatus().value(),
                request.getRequestURI(),
                traceId(),
                Instant.now()
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    /**
     * Handles bean-validation failures on @Valid request bodies, surfacing every failed field.
     *
     * @param ex      the validation exception Spring raised
     * @param request the current request, used to read the request path
     * @return a 400 VALIDATION_FAILED ErrorResponse listing each failed field
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldErrorItem> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorItem(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ErrorResponse body = new ErrorResponse(
                "VALIDATION_FAILED",
                "Request contains invalid fields.",
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                traceId(),
                Instant.now(),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Catch-all for anything not handled above — never leaks internal exception detail to the
     * caller, only the traceId needed to find it in logs.
     *
     * @param ex      the unhandled exception
     * @param request the current request, used to read the request path
     * @return a 500 INTERNAL_ERROR ErrorResponse
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI(),
                traceId(),
                Instant.now()
        );
        return ResponseEntity.internalServerError().body(body);
    }

    /**
     * Reads the current request's correlation id from MDC (set by {@link CorrelationIdFilter}),
     * falling back to a freshly generated id if none is present.
     *
     * @return the trace id to attach to an error response
     */
    private String traceId() {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        return correlationId != null ? correlationId : UUID.randomUUID().toString().replace("-", "");
    }
}
