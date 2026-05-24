package com.company.finance_api.shared.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.company.finance_api.ai.config.AiConfigurationException;
import com.company.finance_api.ai.config.AiDisabledException;
import com.company.finance_api.ai.config.AiOpenAiException;
import com.company.finance_api.ai.config.AiOpenAiQuotaException;
import com.company.finance_api.registration.EmailAvailabilityException;
import jakarta.servlet.ServletException;
import jakarta.transaction.RollbackException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler(false);
  private final GlobalExceptionHandler verboseHandler = new GlobalExceptionHandler(true);

  @Test
  void handleNotFound_should_return404Payload() {
    ApiResponse<Void> response =
        handler.handleNotFound(new ResourceNotFoundException("Alarm rule not found: 42"));

    assertFalse(response.isSuccess());
    assertNotNull(response.getError());
    assertEquals("NOT_FOUND", response.getError().getCode());
    assertEquals("Alarm rule not found: 42", response.getError().getMessage());
  }

  @Test
  void handleNoResourceFound_should_return404Payload() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleNoResourceFound(new NoResourceFoundException(HttpMethod.GET, "/missing"));

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertFalse(response.getBody().isSuccess());
    assertEquals("NOT_FOUND", response.getBody().getError().getCode());
    assertEquals(
        "No handler or static resource for: /missing", response.getBody().getError().getMessage());
  }

  @Test
  void handleValidation_should_return400Payload() {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "email", "must be a well-formed email address"));
    MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

    ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertFalse(response.getBody().isSuccess());
    assertEquals("VALIDATION_ERROR", response.getBody().getError().getCode());
    assertEquals("email: must be a well-formed email address", response.getBody().getError().getMessage());
  }

  @Test
  void handleBadRequest_should_return400WithBadRequestCode() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleBadRequest(new IllegalArgumentException("Instrument not found"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("BAD_REQUEST", response.getBody().getError().getCode());
    assertEquals("Instrument not found", response.getBody().getError().getMessage());
  }

  @Test
  void handleResponseStatus_should_mapStatusAndReason() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleResponseStatus(
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertEquals("401 UNAUTHORIZED", response.getBody().getError().getCode());
    assertEquals("Invalid credentials", response.getBody().getError().getMessage());
  }

  @Test
  void handleRuntime_should_return400ForUnexpectedRuntime() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleRuntime(new UnsupportedOperationException("not supported yet"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("RUNTIME_ERROR", response.getBody().getError().getCode());
    assertEquals("not supported yet", response.getBody().getError().getMessage());
  }

  @Test
  void handleException_should_hideInternalDetailsByDefault() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleException(new IllegalStateException("database password leaked"));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals("INTERNAL_ERROR", response.getBody().getError().getCode());
    assertEquals("Unexpected error occurred", response.getBody().getError().getMessage());
  }

  @Test
  void handleException_should_exposeDetailsWhenConfigured() {
    ResponseEntity<ApiResponse<Void>> response =
        verboseHandler.handleException(new IllegalStateException("database password leaked"));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals("INTERNAL_ERROR", response.getBody().getError().getCode());
    assertEquals("IllegalStateException: database password leaked", response.getBody().getError().getMessage());
  }

  @Test
  void handleSqlException_should_notLeakMessageByDefault() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleSqlException(new SQLException("relation users_secret does not exist"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("SQL_ERROR", response.getBody().getError().getCode());
    assertEquals("Database error", response.getBody().getError().getMessage());
  }

  @Test
  void handleIOException_should_notLeakMessageByDefault() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleIOException(new IOException("/etc/passwd not readable"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("IO_ERROR", response.getBody().getError().getCode());
    assertEquals("Request I/O failed", response.getBody().getError().getMessage());
  }

  @Test
  void handleDataAccess_should_notLeakMessageByDefault() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleDataAccess(
            new InvalidDataAccessResourceUsageException("column ssn_hash does not exist"));

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertEquals("DATA_ACCESS", response.getBody().getError().getCode());
    assertEquals("Database temporarily unavailable", response.getBody().getError().getMessage());
  }

  @Test
  void handleDataIntegrity_should_returnConflictWithSafeMessage() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleDataIntegrity(
            new DataIntegrityViolationException("fk_portfolio_user violates constraint"));

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("DATA_INTEGRITY", response.getBody().getError().getCode());
    assertEquals(
        "The change conflicts with existing data (for example a foreign key). Try again after refreshing.",
        response.getBody().getError().getMessage());
  }

  @Test
  void handleRollback_should_return409() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleRollback(new RollbackException("transaction aborted"));

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("TRANSACTION_ROLLBACK", response.getBody().getError().getCode());
    assertEquals("transaction aborted", response.getBody().getError().getMessage());
  }

  @Test
  void handleIllegalState_should_return409Payload() {
    ApiResponse<Void> response = handler.handleIllegalState(new IllegalStateException("already closed"));

    assertEquals("CONFLICT_ERROR", response.getError().getCode());
    assertEquals("already closed", response.getError().getMessage());
  }

  @Test
  void handleAccessDenied_should_return403Payload() {
    ApiResponse<Void> response =
        handler.handleAccessDenied(new AccessDeniedBusinessException("portfolio not owned"));

    assertEquals("ACCESS_DENIED", response.getError().getCode());
    assertEquals("portfolio not owned", response.getError().getMessage());
  }

  @Test
  void handleAiDisabled_should_return503() {
    ResponseEntity<ApiResponse<Void>> response = handler.handleAiDisabled(new AiDisabledException());

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertEquals("AI_DISABLED", response.getBody().getError().getCode());
  }

  @Test
  void handleAiOpenAi_should_maskProviderDetails() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleAiOpenAi(new AiOpenAiException("secret api key invalid"));

    assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    assertEquals("AI_OPENAI_ERROR", response.getBody().getError().getCode());
    assertEquals("AI response could not be retrieved", response.getBody().getError().getMessage());
  }

  @Test
  void handleEmailAvailability_should_mapBlockedEmailTo403() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleEmailAvailability(EmailAvailabilityException.blocked("blocked@example.com"));

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertEquals("EMAIL_BLOCKED", response.getBody().getError().getCode());
  }

  @Test
  void handleEmailAvailability_should_mapInUseEmailTo409WithSuggestions() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleEmailAvailability(
            EmailAvailabilityException.inUse(List.of("alt1@example.com", "alt2@example.com")));

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("EMAIL_IN_USE", response.getBody().getError().getCode());
    assertEquals(2, response.getBody().getError().getSuggestions().size());
  }

  @Test
  void handleServletException_should_return400WithRequestErrorCode() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleServletException(new ServletException("multipart boundary missing"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("REQUEST_ERROR", response.getBody().getError().getCode());
    assertEquals("multipart boundary missing", response.getBody().getError().getMessage());
  }

  @Test
  void handleAiConfiguration_should_return503() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleAiConfiguration(new AiConfigurationException("OpenAI API key missing"));

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertEquals("AI_CONFIGURATION", response.getBody().getError().getCode());
    assertEquals("OpenAI API key missing", response.getBody().getError().getMessage());
  }

  @Test
  void handleAiOpenAiQuota_should_return503() {
    ResponseEntity<ApiResponse<Void>> response = handler.handleAiOpenAiQuota(new AiOpenAiQuotaException());

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertEquals("OPENAI_QUOTA_EXCEEDED", response.getBody().getError().getCode());
  }
}
