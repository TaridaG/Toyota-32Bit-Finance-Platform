package com.company.finance_api.dto;

import jakarta.validation.constraints.NotBlank;

/** PublicRefreshRequest — API transfer nesnesi (DTO/response/request). */
public class PublicRefreshRequest {

  @NotBlank private String refreshToken;

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
