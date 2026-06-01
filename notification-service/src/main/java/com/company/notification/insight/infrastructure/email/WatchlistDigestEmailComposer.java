package com.company.notification.insight.infrastructure.email;

import com.company.notification.bootstrap.config.NotificationMailProperties;
import com.company.notification.insight.domain.PendingInsightEvent;
import com.company.notification.shared.email.AlarmMailLocale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/**
 * Watchlist fiyat/haber digest e-postaları için yerelleştirilmiş HTML ve plain-text gövde oluşturur.
 */
@Service
public class WatchlistDigestEmailComposer {

    private static final String LOGO_CONTENT_ID = "portalLogo";
    private static final int MAX_TITLE_LEN = 120;

    private final NotificationMailProperties mailProperties;

    public WatchlistDigestEmailComposer(NotificationMailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    public String subjectForLocale(String preferredLocale) {
        String lang = AlarmMailLocale.normalize(preferredLocale);
        return Copy.forLocale(lang).subject();
    }

    public WatchlistDigestEmailContent build(String preferredLocale, List<PendingInsightEvent> topEvents) {
        String lang = AlarmMailLocale.normalize(preferredLocale);
        Copy copy = Copy.forLocale(lang);
        String rowsHtml = buildRowsHtml(topEvents, lang);
        String rowsPlain = buildRowsPlain(topEvents, lang);

        String portalUrl = mailProperties.getPortalPublicUrl();
        String footerLink = StringUtils.hasText(portalUrl)
                ? "<a href=\"%s\" style=\"color:#f59e0b;text-decoration:none;font-weight:600;\">%s</a>"
                        .formatted(escapeHtml(portalUrl.trim()), escapeHtml(portalUrl.trim()))
                : "";

        String html = """
                <!DOCTYPE html>
                <html lang="%s">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background-color:#e2e8f0;font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#e2e8f0;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background-color:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #cbd5e1;box-shadow:0 12px 32px rgba(15,23,42,0.08);">
                          <tr>
                            <td style="background:linear-gradient(135deg,#0f172a 0%%,#1e293b 100%%);padding:28px 24px;text-align:center;">
                              <img src="cid:%s" alt="%s" width="56" height="56" style="display:block;margin:0 auto 14px;border-radius:14px;border:2px solid rgba(245,158,11,0.45);"/>
                              <p style="margin:0;font-size:22px;font-weight:700;color:#f8fafc;letter-spacing:0.02em;">%s</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px 28px 24px;">
                              <p style="margin:0 0 12px;font-size:15px;line-height:1.55;color:#334155;">%s</p>
                              <p style="margin:0 0 20px;font-size:14px;line-height:1.5;color:#64748b;">%s</p>
                              %s
                            </td>
                          </tr>
                          <tr>
                            <td style="background-color:#f8fafc;padding:20px 28px;border-top:1px solid #e2e8f0;text-align:center;">
                              <p style="margin:0 0 8px;font-size:12px;line-height:1.5;color:#94a3b8;">%s</p>
                              %s
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                lang,
                escapeHtml(copy.subject()),
                LOGO_CONTENT_ID,
                escapeHtml(copy.brandName()),
                escapeHtml(copy.brandName()),
                escapeHtml(copy.intro()),
                escapeHtml(copy.instruction()),
                rowsHtml,
                escapeHtml(copy.footer()),
                StringUtils.hasText(footerLink)
                        ? "<p style=\"margin:0;font-size:12px;line-height:1.5;color:#94a3b8;\">%s %s</p>"
                                .formatted(escapeHtml(copy.footerVisit()), footerLink)
                        : "");

        String plain = """
                %s

                %s

                %s

                %s

                %s
                """.formatted(
                copy.brandName(),
                copy.intro(),
                copy.instruction(),
                rowsPlain,
                copy.footer()).trim();

        return new WatchlistDigestEmailContent(copy.subject(), html, plain);
    }

    private String buildRowsHtml(List<PendingInsightEvent> topEvents, String lang) {
        if (topEvents == null || topEvents.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;">
                """);
        for (PendingInsightEvent event : topEvents) {
            sb.append("""
                    <tr>
                      <td style="padding:12px 16px;border-bottom:1px solid #e2e8f0;font-size:14px;color:#334155;">
                        <strong>%s</strong> — %s
                      </td>
                    </tr>
                    """.formatted(escapeHtml(event.getSymbol()), escapeHtml(formatLine(event, lang))));
        }
        sb.append("</table>");
        return sb.toString();
    }

    private String buildRowsPlain(List<PendingInsightEvent> topEvents, String lang) {
        if (topEvents == null || topEvents.isEmpty()) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        for (PendingInsightEvent event : topEvents) {
            sb.append("- ").append(event.getSymbol()).append(": ").append(formatLine(event, lang)).append('\n');
        }
        return sb.toString().trim();
    }

    private static String formatLine(PendingInsightEvent event, String lang) {
        if ("NEWS".equalsIgnoreCase(event.getEventType())) {
            String title = event.getNewsTitle() == null ? "" : event.getNewsTitle().trim();
            if (title.length() > MAX_TITLE_LEN) {
                title = title.substring(0, MAX_TITLE_LEN - 1) + "…";
            }
            return switch (lang) {
                case "tr" -> "Haber: " + (title.isEmpty() ? "—" : title);
                case "de" -> "Nachricht: " + (title.isEmpty() ? "—" : title);
                default -> "News: " + (title.isEmpty() ? "—" : title);
            };
        }
        BigDecimal change = event.getChangePercent() == null ? BigDecimal.ZERO : event.getChangePercent();
        String pct = change.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP) + "%";
        String direction = event.getDirection() == null ? "" : event.getDirection().trim().toUpperCase(Locale.ROOT);
        return switch (direction) {
            case "UP" -> switch (lang) {
                case "tr" -> "Yükseliş " + pct;
                case "de" -> "Anstieg " + pct;
                default -> "Up " + pct;
            };
            case "DOWN" -> switch (lang) {
                case "tr" -> "Düşüş " + pct;
                case "de" -> "Rückgang " + pct;
                default -> "Down " + pct;
            };
            default -> pct;
        };
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private record Copy(
            String lang,
            String subject,
            String brandName,
            String intro,
            String instruction,
            String footer,
            String footerVisit
    ) {
        static Copy forLocale(String lang) {
            return switch (lang) {
                case "tr" -> new Copy(
                        lang,
                        "Takip listenizde güncellemeler",
                        "Finans Portalı",
                        "Merhaba,",
                        "Takip ettiğiniz varlıklarda öne çıkan hareket ve haber özeti:",
                        "Bu mesaj bilgilendirme amaçlıdır; yatırım tavsiyesi değildir.",
                        "Portal:");
                case "de" -> new Copy(
                        lang,
                        "Updates zu Ihrer Beobachtungsliste",
                        "Finanzportal",
                        "Hallo,",
                        "Ausgewählte Kursbewegungen und Nachrichten zu Ihren beobachteten Werten:",
                        "Diese Nachricht dient nur zur Information und ist keine Anlageberatung.",
                        "Portal:");
                default -> new Copy(
                        "en",
                        "Updates on your watchlist",
                        "Finance Portal",
                        "Hello,",
                        "Highlights from price moves and news on instruments you follow:",
                        "This message is for information only and is not investment advice.",
                        "Visit:");
            };
        }
    }
}
