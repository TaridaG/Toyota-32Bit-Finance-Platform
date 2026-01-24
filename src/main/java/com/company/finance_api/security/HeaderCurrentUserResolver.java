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
        String userId = request.getHeader(USER_HEADER);
        if (userId == null) {
            throw new IllegalStateException("Missing X-USER-ID header");
        }
        return UUID.fromString(userId);
    }
}
