package com.company.finance_api.registration;

import java.util.List;
import org.springframework.http.HttpStatus;

/** E-posta müsait değil veya engelli olduğunda fırlatılan domain exception. */
public class EmailAvailabilityException extends RuntimeException {

  public static final String CODE_BLOCKED = "EMAIL_BLOCKED";
  public static final String CODE_IN_USE = "EMAIL_IN_USE";

  private final HttpStatus status;
  private final String errorCode;
  private final List<String> suggestions;

  private EmailAvailabilityException(
      HttpStatus status, String errorCode, String message, List<String> suggestions) {
    super(message);
    this.status = status;
    this.errorCode = errorCode;
    this.suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
  }

  /** Engelli e-posta için FORBIDDEN exception. */
  public static EmailAvailabilityException blocked(String email) {
    return new EmailAvailabilityException(
        HttpStatus.FORBIDDEN,
        CODE_BLOCKED,
        "Bu e-posta adresi sistemden engellenmiştir. Yeni hesap oluşturulamaz.",
        List.of());
  }

  /** Kullanımda olan e-posta için CONFLICT exception ve öneriler. */
  public static EmailAvailabilityException inUse(List<String> suggestions) {
    return new EmailAvailabilityException(
        HttpStatus.CONFLICT, CODE_IN_USE, "Bu e-posta adresi zaten kullanılıyor.", suggestions);
  }

  /** HTTP status kodu. */
  public HttpStatus getStatus() {
    return status;
  }

  /** API error code (EMAIL_BLOCKED, EMAIL_IN_USE). */
  public String getErrorCode() {
    return errorCode;
  }

  /** Alternatif e-posta önerileri. */
  public List<String> getSuggestions() {
    return suggestions;
  }
}
