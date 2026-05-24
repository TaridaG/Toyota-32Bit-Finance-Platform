package com.company.finance_api.shared.web;

import com.company.finance_api.ai.config.AiConfigurationException;
import com.company.finance_api.ai.config.AiDisabledException;
import com.company.finance_api.ai.config.AiOpenAiException;
import com.company.finance_api.ai.config.AiOpenAiQuotaException;
import com.company.finance_api.registration.EmailAvailabilityException;
import jakarta.servlet.ServletException;
import jakarta.transaction.RollbackException;
import java.io.IOException;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Controller katmanı exception'larını standart {@link ApiResponse} + HTTP status'a çevirir. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String GENERIC_INTERNAL_ERROR = "Unexpected error occurred";
  private static final String GENERIC_IO_ERROR = "Request I/O failed";
  private static final String GENERIC_SQL_ERROR = "Database error";
  private static final String GENERIC_DATA_ACCESS_ERROR = "Database temporarily unavailable";

  private final boolean exposeInternalErrors;

  public GlobalExceptionHandler(
      @Value("${app.expose-internal-errors:false}") boolean exposeInternalErrors) {
    this.exposeInternalErrors = exposeInternalErrors;
  }

  /** Bean validation hataları → HTTP 400. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .orElse("Validation failed");
    ApiError error = new ApiError("VALIDATION_ERROR", message);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
  }

  /** Kayıt e-posta müsaitlik hataları → yapılandırılmış status ve öneriler. */
  @ExceptionHandler(EmailAvailabilityException.class)
  public ResponseEntity<ApiResponse<Void>> handleEmailAvailability(EmailAvailabilityException ex) {
    ApiError error = new ApiError(ex.getErrorCode(), ex.getMessage(), ex.getSuggestions());
    return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(error));
  }

  /** {@link ResponseStatusException} → ilgili HTTP status. */
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
   * Multipart/encoding hataları genelde checked {@link ServletException} olarak gelir; önceden
   * {@link #handleException} ile 500'e maskeleniyordu.
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

  /** Geçersiz istek argümanları → HTTP 400. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
    ApiError error = new ApiError("BAD_REQUEST", ex.getMessage());
    return ResponseEntity.badRequest().body(ApiResponse.error(error));
  }

  /** Checked IO hataları → HTTP 400. */
  @ExceptionHandler(IOException.class)
  public ResponseEntity<ApiResponse<Void>> handleIOException(IOException ex) {
    log.warn("IO failure during request handling", ex);
    ApiError error = new ApiError("IO_ERROR", clientMessage(GENERIC_IO_ERROR, ex));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
  }

  /** SQL hataları → HTTP 400. */
  @ExceptionHandler(SQLException.class)
  public ResponseEntity<ApiResponse<Void>> handleSqlException(SQLException ex) {
    log.error("SQL failure", ex);
    ApiError error = new ApiError("SQL_ERROR", clientMessage(GENERIC_SQL_ERROR, ex));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
  }

  /**
   * JTA / Spring transaction rollback checked exception'ıdır; önceden {@link #handleException} ile
   * 500'e düşüyordu.
   */
  @ExceptionHandler(RollbackException.class)
  public ResponseEntity<ApiResponse<Void>> handleRollback(RollbackException ex) {
    log.warn("Transaction rolled back", ex);
    String msg =
        StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "Transaction was rolled back";
    ApiError error = new ApiError("TRANSACTION_ROLLBACK", msg);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
  }

  /** {@link IllegalStateException} → HTTP 409. */
  @ExceptionHandler(IllegalStateException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ApiResponse<Void> handleIllegalState(IllegalStateException ex) {
    ApiError error = new ApiError("CONFLICT_ERROR", ex.getMessage());
    return ApiResponse.error(error);
  }

  /** {@link ResourceNotFoundException} → HTTP 404. */
  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiResponse<Void> handleNotFound(ResourceNotFoundException ex) {
    return ApiResponse.error(new ApiError("NOT_FOUND", ex.getMessage()));
  }

  /**
   * Spring MVC: eşleşen controller yok ve static resource lookup başarısız (eksik route veya yanlış
   * path). {@link RuntimeException} handler'ından önce işlenmeli; aksi halde eksik endpoint HTTP
   * 400 döner.
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
    String path = ex.getResourcePath();
    String msg =
        StringUtils.hasText(path)
            ? "No handler or static resource for: " + path
            : "No handler or static resource for this path";
    ApiError error = new ApiError("NOT_FOUND", msg);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
  }

  /** {@link AccessDeniedBusinessException} → HTTP 403. */
  @ExceptionHandler(AccessDeniedBusinessException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ApiResponse<Void> handleAccessDenied(AccessDeniedBusinessException ex) {
    return ApiResponse.error(new ApiError("ACCESS_DENIED", ex.getMessage()));
  }

  /** AI devre dışı → HTTP 503. */
  @ExceptionHandler(AiDisabledException.class)
  public ResponseEntity<ApiResponse<Void>> handleAiDisabled(AiDisabledException ex) {
    ApiError error = new ApiError("AI_DISABLED", ex.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(error));
  }

  /** AI yapılandırma hatası → HTTP 503. */
  @ExceptionHandler(AiConfigurationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAiConfiguration(AiConfigurationException ex) {
    ApiError error = new ApiError("AI_CONFIGURATION", ex.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(error));
  }

  /** OpenAI quota aşımı → HTTP 503. */
  @ExceptionHandler(AiOpenAiQuotaException.class)
  public ResponseEntity<ApiResponse<Void>> handleAiOpenAiQuota(AiOpenAiQuotaException ex) {
    log.warn("OpenAI quota exceeded");
    ApiError error = new ApiError("OPENAI_QUOTA_EXCEEDED", ex.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(error));
  }

  /** OpenAI entegrasyon hatası → HTTP 502. */
  @ExceptionHandler(AiOpenAiException.class)
  public ResponseEntity<ApiResponse<Void>> handleAiOpenAi(AiOpenAiException ex) {
    log.warn("OpenAI integration failure", ex);
    ApiError error = new ApiError("AI_OPENAI_ERROR", "AI response could not be retrieved");
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.error(error));
  }

  /** Veri bütünlüğü ihlali (FK vb.) → HTTP 409. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
    log.warn("Data integrity violation", ex);
    ApiError error =
        new ApiError(
            "DATA_INTEGRITY",
            "The change conflicts with existing data (for example a foreign key). Try again after refreshing.");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
  }

  /**
   * JPA / JDBC hataları {@link RuntimeException} olsa da generic HTTP 400'e maskelenmemeli (bkz.
   * {@link #handleRuntime}).
   */
  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException ex) {
    log.error("Data access failure", ex);
    ApiError error = new ApiError("DATA_ACCESS", clientMessage(GENERIC_DATA_ACCESS_ERROR, ex));
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(error));
  }

  /** Genel {@link RuntimeException} → HTTP 400 (daha spesifik handler'lar önceliklidir). */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex) {
    log.warn(
        "Runtime exception mapped to HTTP 400 (see handler order for more specific types)", ex);
    String msg = ex.getMessage();
    if (!StringUtils.hasText(msg)) {
      msg = ex.getClass().getSimpleName();
    }
    ApiError error = new ApiError("RUNTIME_ERROR", msg);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
  }

  /** Yakalanmamış exception'lar → HTTP 500. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
    log.error("Unhandled exception mapped to 500", ex);
    ApiError error = new ApiError("INTERNAL_ERROR", clientMessage(GENERIC_INTERNAL_ERROR, ex));
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(error));
  }

  private String clientMessage(String genericMessage, Exception ex) {
    if (!exposeInternalErrors) {
      return genericMessage;
    }
    String detail = ex.getMessage();
    if (!StringUtils.hasText(detail)) {
      return ex.getClass().getSimpleName();
    }
    return ex.getClass().getSimpleName() + ": " + detail;
  }
}
