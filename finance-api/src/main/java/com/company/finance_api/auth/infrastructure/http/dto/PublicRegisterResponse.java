package com.company.finance_api.auth.infrastructure.http.dto;

import java.util.UUID;

/** PublicRegisterResponse — API transfer nesnesi (DTO/response/request). */
public class PublicRegisterResponse {

  private final UUID userId;
  private final String username;
  private final String email;

  public PublicRegisterResponse(UUID userId, String username, String email) {
    this.userId = userId;
    this.username = username;
    this.email = email;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getUsername() {
    return username;
  }

  public String getEmail() {
    return email;
  }
}
