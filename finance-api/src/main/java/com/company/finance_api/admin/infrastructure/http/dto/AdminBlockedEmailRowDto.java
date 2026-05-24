package com.company.finance_api.admin.infrastructure.http.dto;

import java.time.Instant;
import java.util.UUID;

/** Engelli e-posta dizininde tek satır. */
public record AdminBlockedEmailRowDto(
    Long id, String email, Instant blockedAt, UUID sourceUserId) {}
