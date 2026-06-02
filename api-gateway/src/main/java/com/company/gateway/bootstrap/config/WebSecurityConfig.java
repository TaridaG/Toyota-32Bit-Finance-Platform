package com.company.gateway.bootstrap.config;

import com.company.gateway.security.infrastructure.KeycloakJwtGrantedAuthoritiesExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Security WebFlux filter chain'leri: public catalog route'ları, JWT resource server ve role tabanlı authorization.
 */
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
        return p.equals("/api/v1/market") || p.startsWith("/api/v1/market/")
                || p.equals("/api/v1/rates") || p.startsWith("/api/v1/rates/")
                || isPublicGuestNewsPath(p)
                || p.equals("/api/v1/instruments") || p.startsWith("/api/v1/instruments/")
                || p.equals("/api/v1/analytics") || p.startsWith("/api/v1/analytics/")
                || p.equals("/api/v1/portal/info-cards") || p.startsWith("/api/v1/portal/info-cards/");
    }

    /** Public news feed/chart/enriched — not authenticated favorites. */
    private static boolean isPublicGuestNewsPath(String p) {
        if (!p.equals("/api/v1/news") && !p.startsWith("/api/v1/news/")) {
            return false;
        }
        return !p.startsWith("/api/v1/news/favorites");
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

    private static boolean isPublicUnauthenticatedGet(ServerWebExchange exchange) {
        if (!HttpMethod.GET.equals(exchange.getRequest().getMethod())) {
            return false;
        }
        String path = exchange.getRequest().getPath().value();
        if (matchesPublicAuthGetPath(path)) {
            return true;
        }
        String uriPath = exchange.getRequest().getURI().getPath();
        return uriPath != null && matchesPublicAuthGetPath(uriPath);
    }

    private static boolean matchesPublicAuthGetPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String p = path;
        if (p.endsWith("/") && p.length() > 1) {
            p = p.substring(0, p.length() - 1);
        }
        return p.startsWith("/api/v1/public/");
    }

    /** Swagger UI + springdoc config (proxied to finance-api; must ignore stale Bearer tokens). */
    private static boolean isPublicSwaggerDocumentationGet(ServerWebExchange exchange) {
        if (!HttpMethod.GET.equals(exchange.getRequest().getMethod())) {
            return false;
        }
        String path = exchange.getRequest().getPath().value();
        if (matchesPublicSwaggerDocumentationPath(path)) {
            return true;
        }
        String uriPath = exchange.getRequest().getURI().getPath();
        return uriPath != null && matchesPublicSwaggerDocumentationPath(uriPath);
    }

    private static boolean matchesPublicSwaggerDocumentationPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String p = path;
        if (p.endsWith("/") && p.length() > 1) {
            p = p.substring(0, p.length() - 1);
        }
        return p.equals("/swagger-ui.html")
                || p.startsWith("/swagger-ui/")
                || p.startsWith("/webjars/")
                || p.equals("/v3/api-docs")
                || p.startsWith("/v3/api-docs/")
                || (p.startsWith("/services/") && p.contains("/v3/api-docs"));
    }

    private static boolean matchesPublicAuthPostPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String p = path;
        if (p.endsWith("/") && p.length() > 1) {
            p = p.substring(0, p.length() - 1);
        }
        return "/api/v1/public/register".equals(p)
                || "/api/v1/public/register/send-code".equals(p)
                || "/api/v1/public/login".equals(p)
                || "/api/v1/public/login/mfa".equals(p)
                || "/api/v1/public/refresh".equals(p);
    }

    /**
     * Public catalog GET route'ları (TCMB rates, portal info-cards). OAuth2/JWT filter'ları devre dışı; stale Bearer 401 üretmez.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityWebFilterChain publicAnonymousCatalogSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                        "/api/v1/rates", "/api/v1/rates/**",
                        "/api/v1/portal/info-cards", "/api/v1/portal/info-cards/**"))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex.anyExchange().permitAll())
                .build();
    }

    /**
     * Ana JWT resource server filter chain: role kuralları, security header'ları ve akıllı Bearer bypass.
     */
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    @Order(100)
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        ReactiveJwtAuthenticationConverter jwtConverter = new ReactiveJwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt ->
                Flux.fromIterable(KeycloakJwtGrantedAuthoritiesExtractor.extract(jwt)));

        ServerBearerTokenAuthenticationConverter defaultBearer = new ServerBearerTokenAuthenticationConverter();

        return http
                .securityMatcher(new NegatedServerWebExchangeMatcher(
                        ServerWebExchangeMatchers.pathMatchers(
                                "/api/v1/rates", "/api/v1/rates/**",
                                "/api/v1/portal/info-cards", "/api/v1/portal/info-cards/**")))
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
                        .pathMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**")
                        .permitAll()
                        .pathMatchers(HttpMethod.GET, "/services/*/v3/api-docs", "/services/*/v3/api-docs/**")
                        .permitAll()
                        .pathMatchers("/", "/health").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                        .pathMatchers(HttpMethod.POST, GatewaySecurityPaths.publicAuthPosts()).permitAll()
                        .pathMatchers(HttpMethod.GET, GatewaySecurityPaths.publicAnonymousGets()).permitAll()
                        .pathMatchers(HttpMethod.GET, GatewaySecurityPaths.publicAuthGets()).permitAll()
                        .pathMatchers(HttpMethod.GET, GatewaySecurityPaths.newsFavorites()).hasAnyRole("USER", "ADMIN")
                        .pathMatchers(HttpMethod.POST, GatewaySecurityPaths.newsFavorites()).hasAnyRole("USER", "ADMIN")
                        .pathMatchers(HttpMethod.DELETE, GatewaySecurityPaths.newsFavorites()).hasAnyRole("USER", "ADMIN")
                        .pathMatchers(HttpMethod.GET, GatewaySecurityPaths.publicNews()).permitAll()
                        .pathMatchers(HttpMethod.GET, GatewaySecurityPaths.instruments()).permitAll()
                        .pathMatchers(HttpMethod.GET, GatewaySecurityPaths.analytics()).permitAll()
                        .pathMatchers(HttpMethod.GET, GatewaySecurityPaths.portalInfoCards()).permitAll()
                        .pathMatchers(HttpMethod.POST, GatewaySecurityPaths.ingestAdminActions()).hasRole("ADMIN")
                        .pathMatchers(GatewaySecurityPaths.newsAdmin()).hasRole("ADMIN")
                        .pathMatchers(GatewaySecurityPaths.admin()).hasRole("ADMIN")
                        .pathMatchers(GatewaySecurityPaths.userProfile()).hasAnyRole("USER", "ADMIN")
                        .pathMatchers(GatewaySecurityPaths.portfolioWrites()).hasAnyRole("USER", "ADMIN")
                        .pathMatchers("/public/**").permitAll()
                        .pathMatchers("/fallback/**").permitAll()
                        .pathMatchers(GatewaySecurityPaths.versionedApi()).hasAnyRole("USER", "ADMIN")
                        .pathMatchers("/actuator/**").authenticated()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenConverter(exchange -> isPublicUnauthenticatedPost(exchange)
                                        || isPublicUnauthenticatedGet(exchange)
                                        || isPublicAnonymousGet(exchange)
                                        || isPublicSwaggerDocumentationGet(exchange)
                                ? Mono.empty()
                                : defaultBearer.convert(exchange))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                )
                .build();
    }
}
