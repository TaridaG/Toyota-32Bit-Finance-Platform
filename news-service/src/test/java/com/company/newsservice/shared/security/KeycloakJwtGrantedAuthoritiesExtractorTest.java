package com.company.newsservice.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakJwtGrantedAuthoritiesExtractorTest {

    @Test
    void extract_mapsRealmAndClientRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("admin", "user")))
                .claim("resource_access", Map.of(
                        "finance-api", Map.of("roles", List.of("viewer"))
                ))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        Collection<GrantedAuthority> authorities = KeycloakJwtGrantedAuthoritiesExtractor.extract(jwt);

        assertTrue(authorities.stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
        assertTrue(authorities.stream().anyMatch(a -> "ROLE_USER".equals(a.getAuthority())));
        assertTrue(authorities.stream().anyMatch(a -> "ROLE_VIEWER".equals(a.getAuthority())));
    }

    @Test
    void extract_normalizesScopeTokens() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("scope", "openid profile ROLE_trader")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        Collection<GrantedAuthority> authorities = KeycloakJwtGrantedAuthoritiesExtractor.extract(jwt);

        assertTrue(authorities.stream().anyMatch(a -> "ROLE_TRADER".equals(a.getAuthority())));
    }
}
