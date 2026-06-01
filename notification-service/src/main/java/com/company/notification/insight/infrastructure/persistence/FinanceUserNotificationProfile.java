package com.company.notification.insight.infrastructure.persistence;

import java.util.UUID;

/**
 * {@code users} tablosundan okunan bildirim teslimat profili (e-posta, locale, tercihler).
 */
public record FinanceUserNotificationProfile(
        UUID userId,
        String email,
        String preferredLocale,
        boolean active,
        boolean notifyWatchlistAlerts
) {
}
