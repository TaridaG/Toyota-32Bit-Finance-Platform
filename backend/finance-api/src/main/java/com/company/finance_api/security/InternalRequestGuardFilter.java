package com.company.finance_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalRequestGuardFilter extends OncePerRequestFilter {

    private static final String USER_HEADER = "X-USERNAME";

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

        String username = request.getHeader(USER_HEADER);

        if (username == null || username.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing required header: X-USERNAME");
            return;
        }

        filterChain.doFilter(request, response);
    }
}