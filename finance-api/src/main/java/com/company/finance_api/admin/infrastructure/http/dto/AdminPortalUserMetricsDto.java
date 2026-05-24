package com.company.finance_api.admin.infrastructure.http.dto;

import java.time.Instant;
import java.util.List;

/** Admin dashboard için users tablosundan toplanan portal kullanıcı metrikleri. */
public record AdminPortalUserMetricsDto(
    long totalUsers,
    long newUsersLast7Days,
    long newUsersPrevious7Days,
    double newUsersWeekOverWeekPercent,
    List<Integer> newRegistrationsDailyLast7Utc,
    /**
     * O UTC gününde hesap silme sürecini başlatan kullanıcılar ({@link
     * #newRegistrationsDailyLast7Utc} ile paralel).
     */
    List<Integer> userDeletionRequestsDailyLast7Utc,
    Instant generatedAt) {}
