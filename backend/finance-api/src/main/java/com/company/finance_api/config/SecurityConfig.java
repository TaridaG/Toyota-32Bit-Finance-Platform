package com.company.finance_api.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
public class SecurityConfig {

    /**
     * Public TCMB rates proxy ({@code /api/rates/...}). Kept separate from the OAuth2 chain so JWT filters never run
     * here (avoids 401 + {@code WWW-Authenticate: Bearer} for guests and for invalid/expired tokens).
     */
    private static final RequestMatcher PUBLIC_RATES_PATHS = new OrRequestMatcher(
            new AntPathRequestMatcher("/api/rates"),
            new AntPathRequestMatcher("/api/rates/**"));

    private final ObjectProvider<JwtDecoder> jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String jwtIssuerUri;

    public SecurityConfig(ObjectProvider<JwtDecoder> jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    /**
     * TCMB policy rate is a public catalog read (proxied to MDS). This chain runs before the main OAuth2 chain so a
     * stale {@code Authorization: Bearer} from the SPA does not trigger JWT validation and 401 for guests.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain publicRatesSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(PUBLIC_RATES_PATHS)
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(100)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        boolean oauth2Enabled = StringUtils.hasText(jwtIssuerUri) || jwtDecoder.getIfAvailable() != null;

        http
                .securityMatcher(new NegatedRequestMatcher(PUBLIC_RATES_PATHS))
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
