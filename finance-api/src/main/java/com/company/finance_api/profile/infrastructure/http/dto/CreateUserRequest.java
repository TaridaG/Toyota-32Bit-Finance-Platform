package com.company.finance_api.profile.infrastructure.http.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** CreateUserRequest — API transfer nesnesi (DTO/response/request). */
public class CreateUserRequest {

  @Email @NotBlank private String email;

  @NotBlank
  @Size(min = 3, max = 30)
  private String username;

  public String getEmail() {
    return email;
  }

  public String getUsername() {
    return username;
  }
}
