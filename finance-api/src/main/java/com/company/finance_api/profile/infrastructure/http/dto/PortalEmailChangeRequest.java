package com.company.finance_api.profile.infrastructure.http.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** PortalEmailChangeRequest — API transfer nesnesi (DTO/response/request). */
public class PortalEmailChangeRequest {

  @NotBlank @Email private String newEmail;

  public String getNewEmail() {
    return newEmail;
  }

  public void setNewEmail(String newEmail) {
    this.newEmail = newEmail;
  }
}
