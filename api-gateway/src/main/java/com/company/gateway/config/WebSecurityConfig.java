package com.company.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class WebSecurityConfig {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> {}) // CORS bean aşağıda gelecek
                .headers(h -> h
                        .contentTypeOptions(c -> {})
                        .frameOptions(f -> f.mode(XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                        .referrerPolicy(r -> r.policy(org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.NO_REFERRER))
                        .xssProtection(x -> x.disable()) // modern tarayıcılar deprecated, gerçek XSS front/back input validation
                        .hsts(hsts -> hsts.includeSubdomains(true).preload(true).maxAge(java.time.Duration.ofDays(365)))
                )
                .authorizeExchange(ex -> ex
                        .pathMatchers("/actuator/**", "/health").permitAll()

                        // örnek admin path (ileride finance-api admin endpoint açarsın)
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")

                        // geri kalan API: USER veya ADMIN authenticated
                        .pathMatchers("/api/**", "/market/**").hasAnyRole("USER", "ADMIN")

                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(reactiveJwtAuthConverter()))
                )
                .build();
    }


    public Converter<Jwt, Mono<AbstractAuthenticationToken>> reactiveJwtAuthConverter() {
        return jwt -> Mono.just(new JwtAuthenticationToken(jwt, extractAuthorities(jwt)));
    }

    private Collection<org.springframework.security.core.GrantedAuthority> extractAuthorities(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof java.util.Map<?, ?> ra)) {
            return List.of();
        }
        Object roles = ra.get("roles");
        if (!(roles instanceof List<?> list)) {
            return List.of();
        }

        return list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(r -> "ROLE_" + r)
                .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }
}