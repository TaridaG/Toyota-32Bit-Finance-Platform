package com.company.finance_api.dto;

import jakarta.validation.constraints.NotBlank;

/** PortalDeleteAccountRequest — API transfer nesnesi (DTO/response/request). */
public class PortalDeleteAccountRequest {

  @NotBlank private String currentPassword;

  public String getCurrentPassword() {
    return currentPassword;
  }

  public void setCurrentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
  }
}
