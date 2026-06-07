package com.company.finance_api.auth.infrastructure.http.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PublicPasswordResetRequest — anonim şifre sıfırlama isteği. */
public class PublicPasswordResetRequest {

  @NotBlank @Email private String email;

  @NotBlank private String verificationCode;

  @NotBlank
  @Size(min = 8, max = 128)
  private String newPassword;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getVerificationCode() {
    return verificationCode;
  }

  public void setVerificationCode(String verificationCode) {
    this.verificationCode = verificationCode;
  }

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }
}
