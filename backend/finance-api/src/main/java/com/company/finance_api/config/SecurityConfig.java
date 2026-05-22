package com.company.finance_api.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Public catalog reads without JWT (rates, portal info-cards). Separate from the OAuth2 chain so guests and stale
     * Bearer tokens never get {@code 401} + {@code WWW-Authenticate: Bearer}.
     */
    private static final RequestMatcher PUBLIC_ANONYMOUS_READ_PATHS = new OrRequestMatcher(
            new AntPathRequestMatcher("/api/rates"),
            new AntPathRequestMatcher("/api/rates/**"),
            new AntPathRequestMatcher("/api/portal/info-cards"),
            new AntPathRequestMatcher("/api/portal/info-cards/**"));

    private final ObjectProvider<JwtDecoder> jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String jwtIssuerUri;

    public SecurityConfig(ObjectProvider<JwtDecoder> jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    /**
     * Public catalog GETs (TCMB rates, Finansal Okuryazarlık info-cards). Runs before the OAuth2 chain.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain publicAnonymousCatalogSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(PUBLIC_ANONYMOUS_READ_PATHS)
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(100)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        boolean oauth2Enabled = StringUtils.hasText(jwtIssuerUri) || jwtDecoder.getIfAvailable() != null;

        http
                .securityMatcher(new NegatedRequestMatcher(PUBLIC_ANONYMOUS_READ_PATHS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/health").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/public/register", "/api/public/register/send-code", "/api/public/login", "/api/public/refresh")
                                    .permitAll();
                    if (oauth2Enabled) {
                        auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
                    } else {
                        auth.requestMatchers("/api/admin/**").permitAll();
                    }
                    auth.anyRequest().permitAll();
                })
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        if (oauth2Enabled) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        }
        return http.build();
    }
}
