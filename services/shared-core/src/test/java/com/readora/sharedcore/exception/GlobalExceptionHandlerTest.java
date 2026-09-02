package com.readora.sharedcore.exception;

import com.readora.sharedcore.dto.ErrorResponse;
import com.readora.sharedcore.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void handleServiceException_mapsToItsOwnStatusAndCode() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/x");
        ServiceException ex = new ServiceException("NOT_FOUND_X", HttpStatus.NOT_FOUND, "No such x");

        ResponseEntity<ErrorResponse> response = handler.handleServiceException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error()).isEqualTo("NOT_FOUND_X");
        assertThat(response.getBody().message()).isEqualTo("No such x");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/x");
    }

    @Test
    void handleServiceException_usesCorrelationIdFromMdcAsTraceId() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "the-correlation-id");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/x");

        ResponseEntity<ErrorResponse> response = handler.handleServiceException(
                new ServiceException("X", HttpStatus.BAD_REQUEST, "bad"), request
        );

        assertThat(response.getBody().traceId()).isEqualTo("the-correlation-id");
    }

    @Test
    void handleServiceException_noCorrelationIdInMdc_generatesAFallbackTraceId() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/x");

        ResponseEntity<ErrorResponse> response = handler.handleServiceException(
                new ServiceException("X", HttpStatus.BAD_REQUEST, "bad"), request
        );

        assertThat(response.getBody().traceId()).isNotBlank();
    }

    @Test
    void handleValidation_mapsFieldErrorsInto400() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/x");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(new FieldError("obj", "field", "must not be blank")));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().fieldErrors()).hasSize(1);
        assertThat(response.getBody().fieldErrors().get(0).field()).isEqualTo("field");
    }

    @Test
    void handleUnexpected_mapsTo500WithoutLeakingDetail() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/x");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new RuntimeException("db down"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).doesNotContain("db down");
    }
}
