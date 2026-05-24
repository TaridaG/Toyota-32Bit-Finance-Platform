package com.company.analytics.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returns404ApiResponse() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleNotFound(new ResourceNotFoundException("symbol missing"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("NOT_FOUND", response.getBody().getError().getCode());
        assertEquals("symbol missing", response.getBody().getError().getMessage());
    }

    @Test
    void handleBadRequest_returns400ApiResponse() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBadRequest(new IllegalArgumentException("symbol is required"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BAD_REQUEST", response.getBody().getError().getCode());
    }

    @Test
    void handleGeneric_returns500WithoutDetails() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleGeneric(new RuntimeException("secret db error"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().getError().getCode());
        assertEquals("An unexpected error occurred", response.getBody().getError().getMessage());
    }

    @Test
    void handleResponseStatus_preservesStatus() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.CONFLICT, "duplicate"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("CONFLICT", response.getBody().getError().getCode());
        assertEquals("duplicate", response.getBody().getError().getMessage());
    }
}
