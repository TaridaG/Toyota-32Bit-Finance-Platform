package com.company.marketdataservice.shared.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller katmanı exception'larını standart {@link ApiResponse} + HTTP status'a çevirir.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String GENERIC_ERROR_MESSAGE = "An unexpected error occurred";

    /** {@link ResourceNotFoundException} → HTTP 404. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ApiError.of("NOT_FOUND", ex.getMessage())));
    }

    /** Geçersiz istek parametreleri → HTTP 400. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiError.of("BAD_REQUEST", ex.getMessage())));
    }

    /** Mevcut {@link ResponseStatusException} kullanımını standart hata gövdesine map eder. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = StringUtils.hasText(ex.getReason()) ? ex.getReason() : status.getReasonPhrase();
        return ResponseEntity.status(status)
                .body(ApiResponse.error(ApiError.of(status.name(), message)));
    }

    /** Bean validation hataları (WebMVC) → HTTP 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation error");

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiError.of("VALIDATION_ERROR", message)));
    }

    /** Bean validation hataları (WebFlux) → HTTP 400. */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiResponse<Void>> handleWebExchangeValidation(WebExchangeBindException ex) {
        String message = ex.getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation error");

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiError.of("VALIDATION_ERROR", message)));
    }

    /** Beklenmeyen exception'lar → HTTP 500 (detay sadece log'da). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ApiError.of("INTERNAL_ERROR", GENERIC_ERROR_MESSAGE)));
    }
}
