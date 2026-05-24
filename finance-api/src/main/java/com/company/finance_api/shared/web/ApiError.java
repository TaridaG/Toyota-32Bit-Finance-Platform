package com.company.finance_api.shared.web;

import java.time.LocalDateTime;
import java.util.List;

/** API hata gövdesi: kod, mesaj, zaman damgası ve opsiyonel öneri listesi. */
public class ApiError {

  private String code;
  private String message;
  private LocalDateTime timestamp;
  private List<String> suggestions = List.of();

  /** Kod ve mesajla hata oluşturur. */
  public ApiError(String code, String message) {
    this(code, message, List.of());
  }

  /** Kod, mesaj ve öneri listesiyle hata oluşturur. */
  public ApiError(String code, String message, List<String> suggestions) {
    this.code = code;
    this.message = message;
    this.timestamp = LocalDateTime.now();
    this.suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public List<String> getSuggestions() {
    return suggestions;
  }
}
