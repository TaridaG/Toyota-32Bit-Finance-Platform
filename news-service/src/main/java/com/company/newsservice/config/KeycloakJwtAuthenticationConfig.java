package com.company.newsservice.config;

import com.company.newsservice.security.KeycloakJwtGrantedAuthoritiesExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.ArrayList;

@Configuration
public class KeycloakJwtAuthenticationConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                (Jwt jwt) -> new ArrayList<>(KeycloakJwtGrantedAuthoritiesExtractor.extract(jwt)));
        return converter;
    }
}
