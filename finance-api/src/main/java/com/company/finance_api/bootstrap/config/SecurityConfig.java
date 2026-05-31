package com.company.finance_api.bootstrap.config;

import com.company.finance_api.shared.security.FrozenAccountFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

/**
 * OAuth2 resource server filter chain'leri: anonim katalog okumaları ve JWT korumalı portal/admin
 * endpoint'leri.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  /**
   * JWT olmadan erişilebilen katalog okumaları (instruments, rates, portal info-cards). OAuth2 chain'den ayrı
   * tutulur; misafir ve eski Bearer token'lar {@code 401} + {@code WWW-Authenticate: Bearer} almaz.
   */
  private static final RequestMatcher PUBLIC_ANONYMOUS_READ_PATHS =
      new OrRequestMatcher(
          new AntPathRequestMatcher("/api/v1/instruments"),
          new AntPathRequestMatcher("/api/v1/instruments/**"),
          new AntPathRequestMatcher("/api/v1/rates"),
          new AntPathRequestMatcher("/api/v1/rates/**"),
          new AntPathRequestMatcher("/api/v1/portal/info-cards"),
          new AntPathRequestMatcher("/api/v1/portal/info-cards/**"));

  private final ObjectProvider<JwtDecoder> jwtDecoder;
  private final JwtAuthenticationConverter jwtAuthenticationConverter;
  private final FrozenAccountFilter frozenAccountFilter;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
  private String jwtIssuerUri;

  public SecurityConfig(
      ObjectProvider<JwtDecoder> jwtDecoder,
      JwtAuthenticationConverter jwtAuthenticationConverter,
      FrozenAccountFilter frozenAccountFilter) {
    this.jwtDecoder = jwtDecoder;
    this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    this.frozenAccountFilter = frozenAccountFilter;
  }

  /**
   * Anonim katalog GET'leri (instruments, TCMB rates, Finansal Okuryazarlık info-cards). OAuth2 chain'den önce
   * çalışır.
   */
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public SecurityFilterChain publicAnonymousCatalogSecurityFilterChain(HttpSecurity http)
      throws Exception {
    http.securityMatcher(PUBLIC_ANONYMOUS_READ_PATHS)
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }

  /**
   * Ana OAuth2 filter chain: public auth endpoint'leri, admin role kontrolü ve frozen account
   * filter.
   */
  @Bean
  @Order(100)
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    boolean oauth2Enabled =
        StringUtils.hasText(jwtIssuerUri) || jwtDecoder.getIfAvailable() != null;

    http.securityMatcher(new NegatedRequestMatcher(PUBLIC_ANONYMOUS_READ_PATHS))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth -> {
              auth.requestMatchers(
                      "/swagger-ui.html",
                      "/swagger-ui/**",
                      "/v3/api-docs",
                      "/v3/api-docs/**")
                  .permitAll()
                  .requestMatchers("/health", "/actuator", "/actuator/**")
                  .permitAll()
                  .requestMatchers(
                      HttpMethod.POST,
                      "/api/v1/public/register",
                      "/api/v1/public/register/send-code",
                      "/api/v1/public/login",
                      "/api/v1/public/login/mfa",
                      "/api/v1/public/refresh")
                  .permitAll();
              if (oauth2Enabled) {
                auth.requestMatchers("/api/v1/admin/**").hasRole("ADMIN");
              } else {
                auth.requestMatchers("/api/v1/admin/**").permitAll();
              }
              auth.anyRequest().permitAll();
            })
        .httpBasic(httpBasic -> httpBasic.disable())
        .formLogin(form -> form.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    if (oauth2Enabled) {
      http.oauth2ResourceServer(
          oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
      http.addFilterAfter(frozenAccountFilter, BearerTokenAuthenticationFilter.class);
    }
    return http.build();
  }
}
