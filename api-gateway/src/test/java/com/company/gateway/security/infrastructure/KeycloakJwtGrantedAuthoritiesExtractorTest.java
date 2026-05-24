package com.company.gateway.security.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakJwtGrantedAuthoritiesExtractorTest {

    @Test
    void extract_mapsRealmRolesToRolePrefix() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("USER", "ADMIN")))
                .build();

        Collection<GrantedAuthority> authorities = KeycloakJwtGrantedAuthoritiesExtractor.extract(jwt);

        assertTrue(authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet())
                .containsAll(List.of("ROLE_USER", "ROLE_ADMIN")));
    }

    @Test
    void extract_mapsScopeTokensSkippingOpenIdClaims() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("scope", "openid profile USER ADMIN")
                .build();

        Collection<GrantedAuthority> authorities = KeycloakJwtGrantedAuthoritiesExtractor.extract(jwt);

        assertTrue(authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet())
                .containsAll(List.of("ROLE_USER", "ROLE_ADMIN")));
    }

    @Test
    void extract_mapsResourceAccessClientRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("resource_access", Map.of(
                        "finance-api", Map.of("roles", List.of("USER")),
                        "other-client", Map.of("roles", List.of("ADMIN"))))
                .build();

        Collection<GrantedAuthority> authorities = KeycloakJwtGrantedAuthoritiesExtractor.extract(jwt);

        assertTrue(authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet())
                .containsAll(List.of("ROLE_USER", "ROLE_ADMIN")));
    }

    @Test
    void extract_normalizesSimpleRoleNames() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("roles", List.of("admin"))
                .build();

        Collection<GrantedAuthority> authorities = KeycloakJwtGrantedAuthoritiesExtractor.extract(jwt);

        assertTrue(authorities.stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
    }
}
