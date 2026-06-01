package com.company.notification.insight.infrastructure.email;

/**
 * Teslimata hazır render edilmiş watchlist digest e-postası.
 */
public record WatchlistDigestEmailContent(
        String subject,
        String htmlBody,
        String plainBody
) {
}
