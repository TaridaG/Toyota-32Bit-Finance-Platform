package com.company.finance_api.exception;

import com.company.finance_api.ai.config.AiConfigurationException;
import com.company.finance_api.ai.config.AiDisabledException;
import com.company.finance_api.ai.config.AiOpenAiException;
import com.company.finance_api.ai.config.AiOpenAiQuotaException;
import com.company.finance_api.common.ApiError;
import com.company.finance_api.common.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.transaction.RollbackException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.sql.SQLException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final boolean exposeInternalErrors;

    public GlobalExceptionHandler(@Value("${app.expose-internal-errors:false}") boolean exposeInternalErrors) {
        this.exposeInternalErrors = exposeInternalErrors;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation failed");
        ApiError error = new ApiError("VALIDATION_ERROR", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
        String reason = ex.getReason();
        if (!StringUtils.hasText(reason)) {
            reason = ex.getStatusCode().toString();
        }
        ApiError error = new ApiError(ex.getStatusCode().toString(), reason);
        return ResponseEntity.status(ex.getStatusCode()).body(ApiResponse.error(error));
    }

    /**
     * Multipart/encoding failures often surface as checked {@link ServletException}s (not
     * {@link RuntimeException}), which previously fell through to {@link #handleException} and masked as 500.
     */
    @ExceptionHandler(ServletException.class)
    public ResponseEntity<ApiResponse<Void>> handleServletException(ServletException ex) {
        Throwable cause = ex.getRootCause() != null ? ex.getRootCause() : ex;
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getMessage();
        }
        if (message == null || message.isBlank()) {
            message = "Request processing failed";
        }
        ApiError error = new ApiError("REQUEST_ERROR", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    /**
     * Checked IO failures (often propagate past {@link RuntimeException} handlers), JDBC {@link SQLException}, etc.
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<Void>> handleIOException(IOException ex) {
        log.warn("IO failure during request handling", ex);
        String msg = StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : ex.getClass().getSimpleName();
        ApiError error = new ApiError("IO_ERROR", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ApiResponse<Void>> handleSqlException(SQLException ex) {
        log.error("SQL failure", ex);
        ApiError error = new ApiError("SQL_ERROR", StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "Database error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    /**
     * JTA / Spring transaction rollbacks are signaled with this checked exception — not a
     * {@link RuntimeException}, so they previously fell through to {@link #handleException} as a generic 500.
     */
    @ExceptionHandler(RollbackException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollback(RollbackException ex) {
        log.warn("Transaction rolled back", ex);
        String msg = StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "Transaction was rolled back";
        ApiError error = new ApiError("TRANSACTION_ROLLBACK", msg);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleIllegalState(IllegalStateException ex) {
        ApiError error = new ApiError("CONFLICT_ERROR", ex.getMessage());
        return ApiResponse.error(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(ResourceNotFoundException ex) {
        return ApiResponse.error(
                new ApiError("NOT_FOUND", ex.getMessage())
        );
    }

    /**
     * Spring MVC: no controller matched and static resource lookup failed (often an outdated
     * deployment missing a newer route, or a wrong path). Must be handled before {@link RuntimeException}
     * so we do not return HTTP 400 for a missing endpoint.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        String path = ex.getResourcePath();
        String msg = StringUtils.hasText(path)
                ? "No handler or static resource for: " + path
                : "No handler or static resource for this path";
        ApiError error = new ApiError("NOT_FOUND", msg);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
    }

    @ExceptionHandler(AccessDeniedBusinessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedBusinessException ex) {
        return ApiResponse.error(
                new ApiError("ACCESS_DENIED", ex.getMessage())
        );
    }

    @ExceptionHandler(AiDisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiDisabled(AiDisabledException ex) {
        ApiError error = new ApiError("AI_DISABLED", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(error));
    }

    @ExceptionHandler(AiConfigurationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiConfiguration(AiConfigurationException ex) {
        ApiError error = new ApiError("AI_CONFIGURATION", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(error));
    }

    @ExceptionHandler(AiOpenAiQuotaException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiOpenAiQuota(AiOpenAiQuotaException ex) {
        log.warn("OpenAI quota exceeded");
        ApiError error = new ApiError("OPENAI_QUOTA_EXCEEDED", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(error));
    }

    @ExceptionHandler(AiOpenAiException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiOpenAi(AiOpenAiException ex) {
        log.warn("OpenAI integration failure", ex);
        ApiError error = new ApiError("AI_OPENAI_ERROR", "AI response could not be retrieved");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.error(error));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        ApiError error = new ApiError(
                "DATA_INTEGRITY",
                "The change conflicts with existing data (for example a foreign key). Try again after refreshing."
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
    }

    /**
     * JPA / JDBC failures are {@link RuntimeException}s but must not be masked as generic HTTP 400
     * (see {@link #handleRuntime}).
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException ex) {
        log.error("Data access failure", ex);
        Throwable root = ex.getMostSpecificCause();
        String detail = root.getMessage();
        if (!StringUtils.hasText(detail)) {
            detail = ex.getMessage();
        }
        if (!StringUtils.hasText(detail)) {
            detail = "Database access error";
        }
        ApiError error = new ApiError("DATA_ACCESS", detail);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(error));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex) {
        log.warn("Runtime exception mapped to HTTP 400 (see handler order for more specific types)", ex);
        String msg = ex.getMessage();
        if (!StringUtils.hasText(msg)) {
            msg = ex.getClass().getSimpleName();
        }
        ApiError error = new ApiError("RUNTIME_ERROR", msg);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unhandled exception mapped to 500", ex);
        String message = "Unexpected error occurred";
        if (exposeInternalErrors) {
            String detail = ex.getMessage();
            message = ex.getClass().getSimpleName()
                    + (StringUtils.hasText(detail) ? ": " + detail : "");
        }
        ApiError error = new ApiError("INTERNAL_ERROR", message);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(error));
    }
}