package com.company.gateway.security.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * Reactive JWT decoder yapılandırması.
 * Sabit JWKS URI kullanır; strict {@code iss} hostname kontrolü yapmaz (finance-api ile aynı yaklaşım).
 */
@Configuration
@ConditionalOnProperty(prefix = "app.security.jwt", name = "jwk-set-uri")
public class MultiIssuerReactiveJwtDecoderConfig {

    /** JWKS endpoint'inden {@link ReactiveJwtDecoder} bean'i oluşturur. */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(@Value("${app.security.jwt.jwk-set-uri}") String jwkSetUri) {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
