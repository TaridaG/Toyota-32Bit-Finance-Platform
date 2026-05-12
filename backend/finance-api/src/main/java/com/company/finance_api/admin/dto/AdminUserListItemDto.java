package com.company.finance_api.admin.dto;

import java.time.Instant;
import java.util.UUID;

/** Row for admin portal user directory (JPQL constructor projection). */
public record AdminUserListItemDto(
        UUID id,
        String username,
        String email,
        boolean emailVerified,
        Instant createdAt,
        boolean hasProfileAvatar,
        long portfolioCount
) {}
