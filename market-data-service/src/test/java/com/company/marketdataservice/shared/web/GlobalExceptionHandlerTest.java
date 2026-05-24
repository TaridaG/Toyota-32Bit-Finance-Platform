package com.company.marketdataservice.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returns404Payload() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleNotFound(new ResourceNotFoundException("Instrument not found: AAPL"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("NOT_FOUND", response.getBody().error().code());
        assertEquals("Instrument not found: AAPL", response.getBody().error().message());
    }

    @Test
    void handleIllegalArgument_returns400Payload() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("symbol is blank"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BAD_REQUEST", response.getBody().error().code());
        assertEquals("symbol is blank", response.getBody().error().message());
    }

    @Test
    void handleResponseStatus_preservesStatusAndReason() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found in catalog: XYZ")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().error().code());
        assertEquals("Instrument not found in catalog: XYZ", response.getBody().error().message());
    }

    @Test
    void handleValidation_returns400Payload() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "from", "must not be null"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().error().code());
        assertEquals("from: must not be null", response.getBody().error().message());
    }

    @Test
    void handleGeneric_returns500WithoutInternalDetails() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleGeneric(new IllegalStateException("database password leaked"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().error().code());
        assertEquals("An unexpected error occurred", response.getBody().error().message());
    }
}
