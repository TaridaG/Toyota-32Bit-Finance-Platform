package com.company.notification.insight.application;

import com.company.notification.insight.domain.PendingInsightEvent;
import com.company.notification.insight.infrastructure.email.WatchlistDigestEmailComposer;
import com.company.notification.insight.infrastructure.kafka.PortalInboxKafkaPublisher;
import com.company.notification.insight.infrastructure.kafka.messaging.PortalInboxDeliverMessage;
import com.company.notification.insight.infrastructure.persistence.FinanceUserNotificationProfile;
import com.company.notification.insight.infrastructure.persistence.FinanceUserNotificationReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kullanıcı bazında birikmiş watchlist insight/haber satırlarını e-posta ve portal kutusuna teslim eder.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliverWatchlistDigestUseCase {

    private static final int TOP_LIMIT = 5;
    private static final int MAX_BODY_LINE = 200;

    private final FinanceUserNotificationReader financeUserNotificationReader;
    private final SendWatchlistDigestEmailUseCase sendWatchlistDigestEmailUseCase;
    private final WatchlistDigestEmailComposer watchlistDigestEmailComposer;
    private final PortalInboxKafkaPublisher portalInboxKafkaPublisher;

    public void deliver(UUID userId, List<PendingInsightEvent> userEvents) {
        if (userId == null || userEvents == null || userEvents.isEmpty()) {
            return;
        }
        Optional<FinanceUserNotificationProfile> profileOpt = financeUserNotificationReader.findById(userId);
        if (profileOpt.isEmpty()) {
            log.warn("WATCHLIST_DIGEST_SKIPPED userId={} reason=user_not_found", userId);
            return;
        }
        FinanceUserNotificationProfile profile = profileOpt.get();
        if (!profile.active() || !profile.notifyWatchlistAlerts()) {
            log.info(
                    "WATCHLIST_DIGEST_SKIPPED userId={} reason=prefs active={} watchlistAlerts={}",
                    userId,
                    profile.active(),
                    profile.notifyWatchlistAlerts());
            return;
        }

        List<PendingInsightEvent> topEvents = topByAbsChange(userEvents);
        String primarySymbol = topEvents.isEmpty() ? "" : topEvents.get(0).getSymbol();
        String portalTitle = digestTitle(profile.preferredLocale());
        String portalBody = buildPortalBody(topEvents, profile.preferredLocale());

        sendWatchlistDigestEmailUseCase.send(profile, topEvents);

        PortalInboxDeliverMessage inbox = new PortalInboxDeliverMessage();
        inbox.setEventId(UUID.randomUUID());
        inbox.setUserId(userId);
        inbox.setTitle(portalTitle);
        inbox.setBody(portalBody);
        inbox.setPrimarySymbol(primarySymbol);
        inbox.setOccurredAt(Instant.now());
        portalInboxKafkaPublisher.publish(inbox);
    }

    static List<PendingInsightEvent> topByAbsChange(List<PendingInsightEvent> userEvents) {
        return userEvents.stream()
                .sorted(Comparator.comparing(
                        e -> e.getChangePercent() == null ? BigDecimal.ZERO : e.getChangePercent().abs(),
                        Comparator.reverseOrder()))
                .limit(TOP_LIMIT)
                .toList();
    }

    private String digestTitle(String preferredLocale) {
        return watchlistDigestEmailComposer.subjectForLocale(preferredLocale);
    }

    private String buildPortalBody(List<PendingInsightEvent> topEvents, String preferredLocale) {
        String raw = preferredLocale == null ? "en" : preferredLocale.trim().toLowerCase(Locale.ROOT);
        final String lang = raw.length() < 2 ? "en" : raw;
        return topEvents.stream()
                .map(e -> formatPortalLine(e, lang))
                .collect(Collectors.joining("\n"));
    }

    private static String formatPortalLine(PendingInsightEvent event, String lang) {
        String symbol = event.getSymbol() == null ? "—" : event.getSymbol();
        if ("NEWS".equalsIgnoreCase(event.getEventType())) {
            String title = event.getNewsTitle() == null ? "" : event.getNewsTitle().trim();
            if (title.length() > MAX_BODY_LINE) {
                title = title.substring(0, MAX_BODY_LINE - 1) + "…";
            }
            return switch (lang) {
                case "tr" -> symbol + " — Haber: " + title;
                case "de" -> symbol + " — Nachricht: " + title;
                default -> symbol + " — News: " + title;
            };
        }
        BigDecimal change = event.getChangePercent() == null ? BigDecimal.ZERO : event.getChangePercent();
        String pct = change.multiply(new BigDecimal("100")).setScale(1, java.math.RoundingMode.HALF_UP) + "%";
        return symbol + " — " + pct;
    }
}
