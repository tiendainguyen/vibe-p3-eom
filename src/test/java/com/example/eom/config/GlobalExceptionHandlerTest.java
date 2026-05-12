package com.example.eom.config;

import com.example.eom.dto.ErrorResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleValidation_returns400WithFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponseDTO> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Validation Failed");
        assertThat(response.getBody().message()).contains("must not be blank");
        assertThat(response.getBody().status()).isEqualTo(400);
    }

    @Test
    void handleNotFound_returns404() {
        ResponseEntity<ErrorResponseDTO> response =
                handler.handleNotFound(new EntityNotFoundException("Order 99 not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("Order 99 not found");
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    void handleConflict_returns409() {
        ResponseEntity<ErrorResponseDTO> response =
                handler.handleConflict(new IllegalStateException("Email already registered"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Conflict");
        assertThat(response.getBody().status()).isEqualTo(409);
    }

    @Test
    void handleBadRequest_returns400() {
        ResponseEntity<ErrorResponseDTO> response =
                handler.handleBadRequest(new IllegalArgumentException("Invalid sort field"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("Invalid sort field");
    }

    @Test
    void handleGeneral_returns500WithOpaqueMessage() {
        ResponseEntity<ErrorResponseDTO> response =
                handler.handleGeneral(new RuntimeException("NullPointerException at line 42"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        // Must not leak internal exception details
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().message()).doesNotContain("NullPointerException");
    }

    @Test
    void errorResponseDTO_hasTimestamp() {
        ResponseEntity<ErrorResponseDTO> response =
                handler.handleNotFound(new EntityNotFoundException("x"));

        assertThat(response.getBody().timestamp()).isNotNull();
    }
}
