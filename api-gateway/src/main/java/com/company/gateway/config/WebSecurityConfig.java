package com.company.gateway.config;

import com.company.gateway.security.KeycloakJwtGrantedAuthoritiesExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
public class WebSecurityConfig {

    /**
     * Public GET routes listed as {@code permitAll} must still ignore a stale Bearer token;
     * otherwise the JWT authentication filter runs first and returns 401 before authorization.
     */
    private static boolean isPublicAnonymousGet(ServerWebExchange exchange) {
        if (!HttpMethod.GET.equals(exchange.getRequest().getMethod())) {
            return false;
        }
        String path = exchange.getRequest().getPath().value();
        if (matchesPublicAnonymousGetPath(path)) {
            return true;
        }
        String uriPath = exchange.getRequest().getURI().getPath();
        return uriPath != null && matchesPublicAnonymousGetPath(uriPath);
    }

    private static boolean matchesPublicAnonymousGetPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String p = path;
        if (p.endsWith("/") && p.length() > 1) {
            p = p.substring(0, p.length() - 1);
        }
        return p.equals("/api/market") || p.startsWith("/api/market/")
                || p.equals("/market") || p.startsWith("/market/")
                || p.equals("/api/news") || p.startsWith("/api/news/")
                || p.equals("/api/instruments") || p.startsWith("/api/instruments/")
                || p.equals("/api/analytics") || p.startsWith("/api/analytics/");
    }

    /**
     * Public POST routes must not fail when a stale {@code Authorization: Bearer} is present:
     * the resource server would validate JWT before {@code permitAll} and return 401.
     */
    private static boolean isPublicUnauthenticatedPost(ServerWebExchange exchange) {
        if (!HttpMethod.POST.equals(exchange.getRequest().getMethod())) {
            return false;
        }
        String path = exchange.getRequest().getPath().value();
        if (matchesPublicAuthPostPath(path)) {
            return true;
        }
        String uriPath = exchange.getRequest().getURI().getPath();
        return uriPath != null && matchesPublicAuthPostPath(uriPath);
    }

    private static boolean matchesPublicAuthPostPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String p = path;
        if (p.endsWith("/") && p.length() > 1) {
            p = p.substring(0, p.length() - 1);
        }
        return "/api/public/register".equals(p)
                || "/api/public/register/send-code".equals(p)
                || "/api/public/login".equals(p)
                || "/api/public/refresh".equals(p);
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        ReactiveJwtAuthenticationConverter jwtConverter = new ReactiveJwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt ->
                Flux.fromIterable(KeycloakJwtGrantedAuthoritiesExtractor.extract(jwt)));

        ServerBearerTokenAuthenticationConverter defaultBearer = new ServerBearerTokenAuthenticationConverter();

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
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
                        .pathMatchers(HttpMethod.POST,
                                "/api/public/register",
                                "/api/public/register/",
                                "/api/public/register/send-code",
                                "/api/public/register/send-code/",
                                "/api/public/login",
                                "/api/public/login/",
                                "/api/public/refresh",
                                "/api/public/refresh/")
                                .permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/market/**", "/market/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/news/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/instruments/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/analytics/**").permitAll()
                        .pathMatchers("/api/news/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/users/me/**", "/api/profile/**").hasAnyRole("USER", "ADMIN")
                        .pathMatchers("/api/portfolio/**", "/api/accounts/**", "/api/balances/**", "/api/transactions/**", "/api/trades/**", "/api/orders/**").hasAnyRole("USER", "ADMIN")
                        .pathMatchers("/public/**").permitAll()
                        .pathMatchers("/fallback/**").permitAll()
                        .pathMatchers("/api/**").hasAnyRole("USER", "ADMIN")
                        .pathMatchers("/actuator/**").authenticated()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenConverter(exchange -> isPublicUnauthenticatedPost(exchange)
                                        || isPublicAnonymousGet(exchange)
                                ? Mono.empty()
                                : defaultBearer.convert(exchange))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                )
                .build();
    }
}
