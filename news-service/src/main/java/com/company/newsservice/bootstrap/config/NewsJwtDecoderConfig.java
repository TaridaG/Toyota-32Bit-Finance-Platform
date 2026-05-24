package com.company.newsservice.bootstrap.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * In-cluster Keycloak JWKS URI'sinden {@link JwtDecoder} oluşturur; {@code iss} claim'ini tek hostname'e bağlamaz (browser :8085 kullanabilir).
 */
@Configuration
@ConditionalOnProperty(prefix = "app.security.jwt", name = "jwk-set-uri")
public class NewsJwtDecoderConfig {

    /** {@code app.security.jwt.jwk-set-uri} üzerinden Nimbus tabanlı JWT decoder bean'i. */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.security.jwt.jwk-set-uri}") String jwkSetUri) {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
