package com.company.finance_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PortalChangePasswordRequest — API transfer nesnesi (DTO/response/request). */
public class PortalChangePasswordRequest {

  @NotBlank private String currentPassword;

  @NotBlank
  @Size(min = 8, max = 128)
  private String newPassword;

  public String getCurrentPassword() {
    return currentPassword;
  }

  public void setCurrentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
  }

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }
}
