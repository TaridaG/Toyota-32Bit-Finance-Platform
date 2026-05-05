package com.company.finance_api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Access token for Authorization: Bearer (same JWT realm as gateway).
 * Refresh token is returned when the identity provider issues one (Keycloak password / refresh grants).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicLoginResponse(
        String accessToken,
        long expiresIn,
        String tokenType,
        String refreshToken,
        Long refreshExpiresIn
) {
    public PublicLoginResponse(String accessToken, long expiresIn, String tokenType) {
        this(accessToken, expiresIn, tokenType, null, null);
    }
}
