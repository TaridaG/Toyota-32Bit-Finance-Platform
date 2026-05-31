package com.company.finance_api.auth.infrastructure.http.dto;

/** PublicSendVerificationCodeResponse — API transfer nesnesi (DTO/response/request). */
public class PublicSendVerificationCodeResponse {
  private final int expiresInSeconds;
  private final int resendInSeconds;

  public PublicSendVerificationCodeResponse(int expiresInSeconds, int resendInSeconds) {
    this.expiresInSeconds = expiresInSeconds;
    this.resendInSeconds = resendInSeconds;
  }

  public int getExpiresInSeconds() {
    return expiresInSeconds;
  }

  public int getResendInSeconds() {
    return resendInSeconds;
  }
}
