package com.company.finance_api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Access token for Authorization: Bearer (same JWT realm as gateway). Refresh token is returned
 * when the identity provider issues one (Keycloak password / refresh grants).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicLoginResponse(
    String status,
    String accessToken,
    Long expiresIn,
    String tokenType,
    String refreshToken,
    Long refreshExpiresIn,
    String mfaChallengeId) {
  public static final String STATUS_COMPLETE = "COMPLETE";
  public static final String STATUS_MFA_REQUIRED = "MFA_REQUIRED";

  public static PublicLoginResponse complete(
      String accessToken,
      long expiresIn,
      String tokenType,
      String refreshToken,
      Long refreshExpiresIn) {
    return new PublicLoginResponse(
        STATUS_COMPLETE, accessToken, expiresIn, tokenType, refreshToken, refreshExpiresIn, null);
  }

  public static PublicLoginResponse mfaRequired(String challengeId) {
    return new PublicLoginResponse(STATUS_MFA_REQUIRED, null, null, null, null, null, challengeId);
  }

  /** Legacy token mapping from Keycloak (status = COMPLETE). */
  public PublicLoginResponse(
      String accessToken,
      long expiresIn,
      String tokenType,
      String refreshToken,
      Long refreshExpiresIn) {
    this(STATUS_COMPLETE, accessToken, expiresIn, tokenType, refreshToken, refreshExpiresIn, null);
  }
}
