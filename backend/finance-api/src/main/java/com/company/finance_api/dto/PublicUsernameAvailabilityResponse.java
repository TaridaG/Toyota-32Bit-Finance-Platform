package com.company.finance_api.dto;

import java.util.List;

public record PublicUsernameAvailabilityResponse(
        String normalizedUsername,
        boolean available,
        List<String> suggestions
) {
}

