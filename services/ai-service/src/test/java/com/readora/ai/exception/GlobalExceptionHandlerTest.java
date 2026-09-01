package com.readora.ai.exception;

import com.readora.ai.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
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

    @Test
    void handleServiceException_mapsToItsOwnStatusAndCode() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/ai/books/x/reader/chat");

        ResponseEntity<ErrorResponse> response = handler.handleServiceException(new BookAccessDeniedException(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().error()).isEqualTo("BOOK_ACCESS_DENIED");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/ai/books/x/reader/chat");
    }

    @Test
    void handleValidation_mapsFieldErrorsInto400() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/ai/chat");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(new FieldError("obj", "message", "must not be blank")));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().fieldErrors()).hasSize(1);
    }

    @Test
    void handleUnexpected_mapsTo500WithoutLeakingDetail() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/ai/chat");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new RuntimeException("vector store down"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).doesNotContain("vector store down");
    }
}
