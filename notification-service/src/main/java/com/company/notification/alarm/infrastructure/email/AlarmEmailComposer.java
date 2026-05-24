package com.company.notification.alarm.infrastructure.email;

import com.company.notification.alarm.infrastructure.kafka.messaging.AlarmTriggeredMessage;
import com.company.notification.bootstrap.config.NotificationMailProperties;
import com.company.notification.shared.email.AlarmMailLocale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Alarm bildirim e-postaları için yerelleştirilmiş HTML ve plain-text gövde oluşturur.
 */
@Service
public class AlarmEmailComposer {

    private static final String LOGO_CONTENT_ID = "portalLogo";
    private static final DateTimeFormatter WHEN_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final NotificationMailProperties mailProperties;

    public AlarmEmailComposer(NotificationMailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    /**
     * Tetiklenmiş bir alarm için subject ve gövde üretir.
     */
    public AlarmEmailContent build(AlarmTriggeredMessage event) {
        String lang = AlarmMailLocale.normalize(event.getPreferredLocale());
        Copy copy = Copy.forLocale(lang);
        String when = event.getTriggeredAt() != null
                ? WHEN_FMT.format(event.getTriggeredAt())
                : "—";
        String condition = conditionLabel(lang, event.getCondition());
        String threshold = formatNumber(event.getThreshold());
        String price = formatNumber(event.getPrice());
        String symbol = event.getInstrumentSymbol() == null ? "—" : event.getInstrumentSymbol();

        String portalUrl = mailProperties.getPortalPublicUrl();
        String footerLink = StringUtils.hasText(portalUrl)
                ? "<a href=\"%s\" style=\"color:#f59e0b;text-decoration:none;font-weight:600;\">%s</a>".formatted(
                escapeHtml(portalUrl.trim()), escapeHtml(portalUrl.trim()))
                : "";

        String highlightRows = """
                <p style="margin:0 0 14px;font-size:26px;font-weight:700;letter-spacing:0.04em;color:#0f172a;">%s</p>
                <p style="margin:0 0 8px;font-size:14px;line-height:1.5;color:#334155;"><strong>%s</strong> %s</p>
                <p style="margin:0 0 8px;font-size:14px;line-height:1.5;color:#334155;"><strong>%s</strong> %s</p>
                <p style="margin:0 0 8px;font-size:14px;line-height:1.5;color:#334155;"><strong>%s</strong> %s</p>
                <p style="margin:0;font-size:14px;line-height:1.5;color:#334155;"><strong>%s</strong> %s</p>
                """.formatted(
                escapeHtml(symbol),
                escapeHtml(copy.conditionField()), escapeHtml(condition),
                escapeHtml(copy.thresholdField()), escapeHtml(threshold),
                escapeHtml(copy.priceField()), escapeHtml(price),
                escapeHtml(copy.whenField()), escapeHtml(when)
        );

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
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                <tr>
                                  <td align="center" style="background-color:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:22px 20px;">
                                    %s
                                  </td>
                                </tr>
                              </table>
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
                escapeHtml(copy.subject().formatted(symbol)),
                LOGO_CONTENT_ID,
                escapeHtml(copy.brandName()),
                escapeHtml(copy.brandName()),
                escapeHtml(copy.intro()),
                escapeHtml(copy.instruction()),
                highlightRows,
                escapeHtml(copy.footer()),
                StringUtils.hasText(footerLink)
                        ? "<p style=\"margin:0;font-size:12px;line-height:1.5;color:#94a3b8;\">%s %s</p>".formatted(
                        escapeHtml(copy.footerVisit()), footerLink)
                        : ""
        );

        String plain = """
                %s

                %s

                %s

                %s: %s
                %s: %s
                %s: %s
                %s: %s
                %s: %s

                %s
                """.formatted(
                copy.brandName(),
                copy.intro(),
                copy.instruction(),
                copy.symbolLabel(), symbol,
                copy.conditionField(), condition,
                copy.thresholdField(), threshold,
                copy.priceField(), price,
                copy.whenField(), when,
                copy.footer()
        ).trim();

        return new AlarmEmailContent(copy.subject().formatted(symbol), html, plain);
    }

    private static String formatNumber(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        return value.stripTrailingZeros().toPlainString();
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

    /**
     * Alarm condition kodunu istenen dilde kısa bir etikete çevirir.
     */
    static String conditionLabel(String lang, String rawCondition) {
        if (rawCondition == null || rawCondition.isBlank()) {
            return "—";
        }
        String key = rawCondition.trim().toUpperCase(Locale.ROOT);
        return switch (key) {
            case "GREATER_THAN" -> switch (lang) {
                case "tr" -> "Fiyat hedefin üzerinde";
                case "de" -> "Kurs über Schwelle";
                default -> "Price above target";
            };
            case "LESS_THAN" -> switch (lang) {
                case "tr" -> "Fiyat hedefin altında";
                case "de" -> "Kurs unter Schwelle";
                default -> "Price below target";
            };
            case "PERCENT_CHANGE_UP" -> switch (lang) {
                case "tr" -> "Yüzde artış";
                case "de" -> "Prozentuale Steigung";
                default -> "Percent increase";
            };
            case "PERCENT_CHANGE_DOWN" -> switch (lang) {
                case "tr" -> "Yüzde düşüş";
                case "de" -> "Prozentualer Rückgang";
                default -> "Percent decrease";
            };
            case "EQUAL" -> switch (lang) {
                case "tr" -> "Fiyat eşit";
                case "de" -> "Kurs gleich Schwelle";
                default -> "Price equals target";
            };
            default -> rawCondition;
        };
    }

    private record Copy(
            String lang,
            String subject,
            String brandName,
            String intro,
            String instruction,
            String symbolLabel,
            String conditionField,
            String thresholdField,
            String priceField,
            String whenField,
            String footer,
            String footerVisit
    ) {
        static Copy forLocale(String lang) {
            return switch (lang) {
                case "tr" -> new Copy(
                        lang,
                        "%s — fiyat alarmınız tetiklendi",
                        "Finans Portalı",
                        "Merhaba,",
                        "Kurduğunuz fiyat alarmı tetiklendi. Özet bilgiler aşağıdadır:",
                        "Sembol",
                        "Koşul",
                        "Alarm fiyatı",
                        "Tetiklenen fiyat",
                        "Zaman",
                        "Bu mesaj bilgilendirme amaçlıdır; yatırım tavsiyesi değildir.",
                        "Portal:"
                );
                case "de" -> new Copy(
                        lang,
                        "%s — Kursalarm ausgelöst",
                        "Finanzportal",
                        "Hallo,",
                        "Ihr Kursalarm wurde ausgelöst. Zusammenfassung:",
                        "Symbol",
                        "Bedingung",
                        "Alarmkurs",
                        "Ausgelöster Kurs",
                        "Zeitpunkt",
                        "Diese Nachricht dient nur zur Information und ist keine Anlageberatung.",
                        "Portal:"
                );
                default -> new Copy(
                        "en",
                        "%s — price alert triggered",
                        "Finance Portal",
                        "Hello,",
                        "Your price alert has been triggered. Summary:",
                        "Symbol",
                        "Condition",
                        "Alert price",
                        "Triggered price",
                        "Time",
                        "This message is for information only and is not investment advice.",
                        "Visit:"
                );
            };
        }
    }
}
