package com.company.finance_api.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class HeaderCurrentUserResolver implements CurrentUserResolver {

    private static final String USER_HEADER = "X-USER-ID";

    private final HttpServletRequest request;

    public HeaderCurrentUserResolver(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public UUID getCurrentUserId() {
        String raw = request.getHeader(USER_HEADER);
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Missing required header: " + USER_HEADER);
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid X-USER-ID format (must be UUID): " + raw);
        }
    }
}
