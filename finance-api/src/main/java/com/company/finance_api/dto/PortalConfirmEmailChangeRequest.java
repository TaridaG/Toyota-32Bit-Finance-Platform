package com.company.finance_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** PortalConfirmEmailChangeRequest — API transfer nesnesi (DTO/response/request). */
public class PortalConfirmEmailChangeRequest {

  @NotBlank @Email private String newEmail;

  @NotBlank private String verificationCode;

  public String getNewEmail() {
    return newEmail;
  }

  public void setNewEmail(String newEmail) {
    this.newEmail = newEmail;
  }

  public String getVerificationCode() {
    return verificationCode;
  }

  public void setVerificationCode(String verificationCode) {
    this.verificationCode = verificationCode;
  }
}
