package com.company.finance_api.auth.infrastructure.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PublicLoginRequest — API transfer nesnesi (DTO/response/request). */
public class PublicLoginRequest {

  @NotBlank
  @Size(max = 255)
  private String username;

  @NotBlank
  @Size(min = 1, max = 256)
  private String password;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
