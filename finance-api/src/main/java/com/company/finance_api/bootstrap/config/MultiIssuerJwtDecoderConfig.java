package com.company.finance_api.bootstrap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * In-cluster Keycloak JWKS URI'sinden {@link JwtDecoder} oluşturur; {@code iss} claim'ini tek
 * hostname'e bağlamaz (Keycloak {@code :8080}, browser {@code :8085} kullanabilir; imza doğrulaması
 * token'ı realm JWKS'e bağlar).
 */
@Configuration
@ConditionalOnProperty(prefix = "app.security.jwt", name = "jwk-set-uri")
public class MultiIssuerJwtDecoderConfig {

  /** {@code app.security.jwt.jwk-set-uri} üzerinden Nimbus tabanlı JWT decoder bean'i. */
  @Bean
  public JwtDecoder jwtDecoder(@Value("${app.security.jwt.jwk-set-uri}") String jwkSetUri) {
    return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
  }
}
