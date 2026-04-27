package com.company.finance_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalRequestGuardFilter extends OncePerRequestFilter {

    private static final String USER_HEADER = "X-USERNAME";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private final boolean allowHeaderUserFallback;

    public InternalRequestGuardFilter(
            @Value("${app.security.allow-header-user-fallback:true}") boolean allowHeaderUserFallback
    ) {
        this.allowHeaderUserFallback = allowHeaderUserFallback;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // health endpoint serbest
        if (path.startsWith("/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (hasJwtAuthentication() || hasBearerTokenHeader(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!allowHeaderUserFallback) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing authenticated user context");
            return;
        }

        String username = request.getHeader(USER_HEADER);

        if (!StringUtils.hasText(username)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing required header: X-USERNAME");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean hasJwtAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof JwtAuthenticationToken;
    }

    private static boolean hasBearerTokenHeader(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        return StringUtils.hasText(authorization) && authorization.regionMatches(true, 0, "Bearer ", 0, 7);
    }
}