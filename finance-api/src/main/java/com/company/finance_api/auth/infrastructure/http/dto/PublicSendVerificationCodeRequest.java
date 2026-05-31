package com.company.finance_api.auth.infrastructure.http.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PublicSendVerificationCodeRequest — API transfer nesnesi (DTO/response/request). */
public class PublicSendVerificationCodeRequest {
  @NotBlank @Email private String email;

  /** Optional UI locale (en, tr, de) from the registration page. */
  @Size(max = 16)
  private String locale;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }
}
