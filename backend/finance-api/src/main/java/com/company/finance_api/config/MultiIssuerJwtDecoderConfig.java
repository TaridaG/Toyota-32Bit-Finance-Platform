package com.company.finance_api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Decode JWTs from a fixed Keycloak JWKS URL. We intentionally do <strong>not</strong> validate
 * {@code iss} against a single hostname: Keycloak may emit {@code iss} on {@code :8080} while the
 * browser uses {@code :8085}; signature verification against this realm's JWKS already binds the token.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.security.jwt", name = "jwk-set-uri")
public class MultiIssuerJwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.security.jwt.jwk-set-uri}") String jwkSetUri) {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
