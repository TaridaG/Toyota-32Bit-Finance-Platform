package com.company.finance_api.admin.infrastructure.http.dto;

import java.time.Instant;
import java.util.UUID;

/** Admin portal kullanıcı dizini satırı (JPQL constructor projection). */
public record AdminUserListItemDto(
    UUID id,
    String username,
    String email,
    boolean emailVerified,
    Instant createdAt,
    boolean hasProfileAvatar,
    long portfolioCount,
    boolean frozen) {}
