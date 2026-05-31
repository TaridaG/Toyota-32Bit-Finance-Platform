package com.company.finance_api.profile.infrastructure.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PortalForgotPasswordResetRequest — API transfer nesnesi (DTO/response/request). */
public class PortalForgotPasswordResetRequest {

  @NotBlank private String verificationCode;

  @NotBlank
  @Size(min = 8, max = 128)
  private String newPassword;

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
