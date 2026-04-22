package com.company.gateway.config;

import com.company.gateway.security.KeycloakJwtGrantedAuthoritiesExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import reactor.core.publisher.Flux;

@Configuration
public class WebSecurityConfig {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        ReactiveJwtAuthenticationConverter jwtConverter = new ReactiveJwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt ->
                Flux.fromIterable(KeycloakJwtGrantedAuthoritiesExtractor.extract(jwt)));

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> {})
                .headers(h -> h
                        .contentTypeOptions(c -> {})
                        .frameOptions(f -> f.mode(XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                        .referrerPolicy(r -> r.policy(org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.NO_REFERRER))
                        .xssProtection(x -> x.disable())
                        .hsts(hsts -> hsts.includeSubdomains(true).preload(true).maxAge(java.time.Duration.ofDays(365)))
                )
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/", "/health").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                        .pathMatchers("/market/**").permitAll()
                        .pathMatchers("/api/news/**").permitAll()
                        .pathMatchers("/public/**").permitAll()
                        .pathMatchers("/fallback/**").permitAll()
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/**").hasAnyRole("USER", "ADMIN")
                        .pathMatchers("/actuator/**").authenticated()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                )
                .build();
    }
}
