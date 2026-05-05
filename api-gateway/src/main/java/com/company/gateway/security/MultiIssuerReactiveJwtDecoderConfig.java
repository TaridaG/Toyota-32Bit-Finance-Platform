package com.company.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * Reactive JWT from fixed JWKS; no strict {@code iss} hostname check (same reasoning as finance-api
 * {@link com.company.finance_api.config.MultiIssuerJwtDecoderConfig}).
 */
@Configuration
@ConditionalOnProperty(prefix = "app.security.jwt", name = "jwk-set-uri")
public class MultiIssuerReactiveJwtDecoderConfig {

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(@Value("${app.security.jwt.jwk-set-uri}") String jwkSetUri) {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
