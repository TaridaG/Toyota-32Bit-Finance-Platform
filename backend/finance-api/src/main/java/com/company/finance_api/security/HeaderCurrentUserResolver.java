package com.company.finance_api.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import com.company.finance_api.domain.User;
import com.company.finance_api.exception.ResourceNotFoundException;
import com.company.finance_api.repository.UserRepository;
import java.util.UUID;

@Component
public class HeaderCurrentUserResolver implements CurrentUserResolver {

    private static final String USERNAME_HEADER = "X-USERNAME";
    private final UserRepository userRepository;
    private final HttpServletRequest request;

    public HeaderCurrentUserResolver(HttpServletRequest request,UserRepository userRepository) {
        this.request = request;
        this.userRepository = userRepository;
    }

    @Override
    public UUID getCurrentUserId() {
        String username = request.getHeader(USERNAME_HEADER);
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Missing required header: " + USERNAME_HEADER);
        }
        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for username: " + username));
        return user.getId();
    }
}
