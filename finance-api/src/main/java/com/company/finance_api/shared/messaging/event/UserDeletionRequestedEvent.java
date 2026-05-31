package com.company.finance_api.shared.messaging.event;

import java.util.UUID;

/** UserDeletionRequestedEvent — domain/Kafka event payload'u (user deletion requested event). */
public record UserDeletionRequestedEvent(UUID userId, String username, String email) {}
