package com.company.finance_api.bootstrap.config;

import com.company.finance_api.shared.security.KeycloakJwtGrantedAuthoritiesExtractor;
import java.util.ArrayList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/** Keycloak JWT claim'lerinden Spring Security {@code GrantedAuthority} dönüşümünü yapılandırır. */
@Configuration
public class KeycloakJwtAuthenticationConfig {

  /**
   * Keycloak realm/client role'lerini {@code ROLE_*} prefix'li authority'lere çeviren converter
   * bean'i.
   */
  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        (Jwt jwt) -> new ArrayList<>(KeycloakJwtGrantedAuthoritiesExtractor.extract(jwt)));
    return converter;
  }
}
