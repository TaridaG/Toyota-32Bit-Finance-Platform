package com.company.newsservice.bootstrap.config;

import com.company.newsservice.shared.security.KeycloakJwtGrantedAuthoritiesExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.ArrayList;

/**
 * Keycloak JWT claim'lerinden Spring Security {@code GrantedAuthority} listesi üreten {@link JwtAuthenticationConverter} bean'i.
 */
@Configuration
public class KeycloakJwtAuthenticationConfig {

    /** Keycloak realm/client rollerini {@code ROLE_*} prefix'i ile authority'ye dönüştürür. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                (Jwt jwt) -> new ArrayList<>(KeycloakJwtGrantedAuthoritiesExtractor.extract(jwt)));
        return converter;
    }
}
