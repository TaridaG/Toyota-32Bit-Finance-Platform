package com.company.finance_api.profile.infrastructure.http.dto;

import jakarta.validation.constraints.Size;

/** PortalUpdatePhoneRequest — API transfer nesnesi (DTO/response/request). */
public class PortalUpdatePhoneRequest {

  /**
   * Null leaves unchanged; empty string clears. Otherwise normalized to digits and optional leading
   * +.
   */
  @Size(max = 32)
  private String phone;

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }
}
