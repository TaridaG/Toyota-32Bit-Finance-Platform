package com.company.notification.insight.application;

import com.company.notification.insight.domain.PendingInsightEvent;
import com.company.notification.insight.infrastructure.email.WatchlistDigestEmailComposer;
import com.company.notification.insight.infrastructure.email.WatchlistDigestEmailContent;
import com.company.notification.insight.infrastructure.persistence.FinanceUserNotificationProfile;
import com.company.notification.shared.email.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Watchlist digest için markalı e-posta gönderir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendWatchlistDigestEmailUseCase {

    private final EmailSender emailSender;
    private final WatchlistDigestEmailComposer watchlistDigestEmailComposer;

    public void send(FinanceUserNotificationProfile profile, List<PendingInsightEvent> topEvents) {
        if (profile == null || !StringUtils.hasText(profile.email())) {
            log.warn("WATCHLIST_DIGEST_EMAIL_SKIPPED userId={} reason=no_user_email", profile == null ? null : profile.userId());
            return;
        }
        try {
            WatchlistDigestEmailContent content =
                    watchlistDigestEmailComposer.build(profile.preferredLocale(), topEvents);
            emailSender.sendBrandedEmail(
                    profile.email().trim(),
                    content.subject(),
                    content.htmlBody(),
                    content.plainBody());
            log.info("WATCHLIST_DIGEST_EMAIL_SENT userId={} lines={}", profile.userId(), topEvents.size());
        } catch (Exception ex) {
            log.error("WATCHLIST_DIGEST_EMAIL_FAILED userId={}", profile.userId(), ex);
            throw ex;
        }
    }
}
