package com.company.finance_api.shared.security;

import com.company.finance_api.domain.User;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * JWT {@code preferred_username} veya opsiyonel {@code X-USERNAME} header fallback ile mevcut
 * kullanıcı UUID'sini çözer.
 */
@Component
public class HeaderCurrentUserResolver implements CurrentUserResolver {

  private static final String USERNAME_HEADER = "X-USERNAME";
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final Logger log = LoggerFactory.getLogger(HeaderCurrentUserResolver.class);
  private static final Set<String> FALLBACK_WARNED_USERS = ConcurrentHashMap.newKeySet();
  private final UserRepository userRepository;
  private final PortalAccountGuardService portalAccountGuardService;
  private final HttpServletRequest request;
  private final MeterRegistry meterRegistry;
  private final boolean allowHeaderUserFallback;

  public HeaderCurrentUserResolver(
      HttpServletRequest request,
      UserRepository userRepository,
      PortalAccountGuardService portalAccountGuardService,
      MeterRegistry meterRegistry,
      @Value("${app.security.allow-header-user-fallback:true}") boolean allowHeaderUserFallback) {
    this.request = request;
    this.userRepository = userRepository;
    this.portalAccountGuardService = portalAccountGuardService;
    this.meterRegistry = meterRegistry;
    this.allowHeaderUserFallback = allowHeaderUserFallback;
  }

  /** {@inheritDoc} JWT öncelikli; fallback kapalıysa veya header yoksa exception fırlatır. */
  @Override
  public UUID getCurrentUserId() {
    String username = resolveJwtUsername();
    if (StringUtils.hasText(username)) {
      return findUserIdByIdentity(username.trim());
    }

    if (!allowHeaderUserFallback) {
      throw new IllegalStateException("Missing authenticated user context");
    }

    String headerUsername = request.getHeader(USERNAME_HEADER);
    if (!StringUtils.hasText(headerUsername)) {
      throw new IllegalStateException("Missing required header: " + USERNAME_HEADER);
    }
    String normalizedUsername = headerUsername.trim();
    String fallbackSource = resolveFallbackSource();
    meterRegistry
        .counter(
            "finance_auth_header_fallback_used_total",
            "service",
            "finance-api",
            "reason",
            "no_jwt",
            "source",
            fallbackSource)
        .increment();
    if (FALLBACK_WARNED_USERS.add(normalizedUsername)) {
      log.warn(
          "FINANCE_AUTH_HEADER_FALLBACK_USED username={} source={}",
          normalizedUsername,
          fallbackSource);
    }
    return findUserIdByIdentity(normalizedUsername);
  }

  private String resolveJwtUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken jwtToken)) {
      return null;
    }
    String preferredUsername = jwtToken.getToken().getClaimAsString("preferred_username");
    if (StringUtils.hasText(preferredUsername)) {
      return preferredUsername;
    }
    String subject = jwtToken.getToken().getSubject();
    return StringUtils.hasText(subject) ? subject : null;
  }

  /**
   * JWT {@code preferred_username} portal display rename sonrası da Keycloak {@code auth_username}
   * üzerinde kalır.
   */
  private UUID findUserIdByIdentity(String identity) {
    String key = identity.trim().toLowerCase(Locale.ROOT);
    User user =
        userRepository
            .findByAuthUsernameIgnoreCase(key)
            .or(() -> userRepository.findByUsernameIgnoreCase(key))
            .or(() -> userRepository.findByEmailIgnoreCase(key))
            .orElseThrow(
                () -> new ResourceNotFoundException("User not found for username: " + identity));
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!portalAccountGuardService.isAdmin(authentication)) {
      portalAccountGuardService.assertNotFrozen(user);
    }
    return user.getId();
  }

  private String resolveFallbackSource() {
    String authorization = request.getHeader(AUTHORIZATION_HEADER);
    if (!StringUtils.hasText(authorization)) {
      return "missing_authorization_header";
    }
    if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return "bearer_absent";
    }
    return "missing_authorization_header";
  }
}
