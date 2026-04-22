package com.company.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@Configuration
@Profile("docker")
public class JwtResourceServerDecoderConfig {

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(
            @Value("${app.security.jwt.issuer-uri}") String issuerUri,
            @Value("${app.security.jwt.jwk-set-uri}") String jwkSetUri
    ) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return decoder;
    }
}
