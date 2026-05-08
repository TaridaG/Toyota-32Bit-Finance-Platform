package com.company.finance_api.event;

import java.util.UUID;

public record UserDeletionRequestedEvent(
        UUID userId,
        String username,
        String email
) {
}

