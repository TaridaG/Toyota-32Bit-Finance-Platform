package com.company.finance_api.auth.infrastructure.http.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PublicRegisterRequest — API transfer nesnesi (DTO/response/request). */
public class PublicRegisterRequest {

  @NotBlank @Email private String email;

  @NotBlank
  @Size(min = 3, max = 36)
  @Pattern(
      regexp = "^[a-zA-Z0-9._-]+$",
      message = "Username may contain letters, digits, dot, underscore, hyphen")
  private String username;

  @NotBlank
  @Size(min = 8, max = 128)
  private String password;

  @NotBlank
  @Size(min = 4, max = 12)
  private String verificationCode;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

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

  public String getVerificationCode() {
    return verificationCode;
  }

  public void setVerificationCode(String verificationCode) {
    this.verificationCode = verificationCode;
  }
}
