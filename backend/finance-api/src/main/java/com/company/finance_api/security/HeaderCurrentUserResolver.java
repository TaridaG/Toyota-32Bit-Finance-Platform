package com.company.finance_api.security;

import jakarta.servlet.http.HttpServletRequest;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import com.company.finance_api.domain.User;
import com.company.finance_api.exception.ResourceNotFoundException;
import com.company.finance_api.repository.UserRepository;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HeaderCurrentUserResolver implements CurrentUserResolver {

    private static final String USERNAME_HEADER = "X-USERNAME";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final Logger log = LoggerFactory.getLogger(HeaderCurrentUserResolver.class);
    private static final Set<String> FALLBACK_WARNED_USERS = ConcurrentHashMap.newKeySet();
    private final UserRepository userRepository;
    private final HttpServletRequest request;
    private final MeterRegistry meterRegistry;
    private final boolean allowHeaderUserFallback;

    public HeaderCurrentUserResolver(
            HttpServletRequest request,
            UserRepository userRepository,
            MeterRegistry meterRegistry,
            @Value("${app.security.allow-header-user-fallback:true}") boolean allowHeaderUserFallback
    ) {
        this.request = request;
        this.userRepository = userRepository;
        this.meterRegistry = meterRegistry;
        this.allowHeaderUserFallback = allowHeaderUserFallback;
    }

    @Override
    public UUID getCurrentUserId() {
        String username = resolveJwtUsername();
        if (StringUtils.hasText(username)) {
            return findUserIdByUsername(username.trim());
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
        meterRegistry.counter(
                "finance_auth_header_fallback_used_total",
                "service", "finance-api",
                "reason", "no_jwt",
                "source", fallbackSource
        ).increment();
        if (FALLBACK_WARNED_USERS.add(normalizedUsername)) {
            log.warn("FINANCE_AUTH_HEADER_FALLBACK_USED username={} source={}", normalizedUsername, fallbackSource);
        }
        return findUserIdByUsername(normalizedUsername);
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

    private UUID findUserIdByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for username: " + username));
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
