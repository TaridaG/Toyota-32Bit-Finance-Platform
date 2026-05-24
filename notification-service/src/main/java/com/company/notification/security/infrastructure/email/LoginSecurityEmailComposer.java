package com.company.notification.security.infrastructure.email;

import com.company.notification.alarm.infrastructure.email.AlarmEmailContent;
import com.company.notification.bootstrap.config.NotificationMailProperties;
import com.company.notification.security.infrastructure.kafka.messaging.LoginSecurityAlertMessage;
import com.company.notification.shared.email.AlarmMailLocale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Desteklenen tüm alert type'lar için yerelleştirilmiş güvenlik bildirim e-postaları oluşturur.
 */
@Service
public class LoginSecurityEmailComposer {

    private static final String LOGO_CONTENT_ID = "portalLogo";
    private static final DateTimeFormatter WHEN_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final NotificationMailProperties mailProperties;

    public LoginSecurityEmailComposer(NotificationMailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    /**
     * Verilen security alert için subject ve HTML/plain içerik üretir.
     */
    public AlarmEmailContent build(LoginSecurityAlertMessage event) {
        String lang = AlarmMailLocale.normalize(event.getPreferredLocale());
        String type = event.getAlertType() == null ? "" : event.getAlertType().trim().toUpperCase();
        Copy copy = Copy.forAlertType(lang, type);
        String when = event.getOccurredAt() != null
                ? WHEN_FMT.format(event.getOccurredAt())
                : "—";
        String ip = StringUtils.hasText(event.getClientIp()) ? event.getClientIp().trim() : copy.unknownValue();
        String device = StringUtils.hasText(event.getUserAgent()) ? event.getUserAgent().trim() : copy.unknownValue();
        String highlightRows = buildHighlightRows(event, copy, when, ip, device);

        String portalUrl = mailProperties.getPortalPublicUrl();
        String footerLink = StringUtils.hasText(portalUrl)
                ? "<a href=\"%s\" style=\"color:#f59e0b;text-decoration:none;font-weight:600;\">%s</a>".formatted(
                escapeHtml(portalUrl.trim()), escapeHtml(portalUrl.trim()))
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
                              <p style="margin:0 0 16px;display:inline-block;padding:6px 12px;border-radius:999px;background:%s;color:%s;font-size:13px;font-weight:600;">%s</p>
                              <p style="margin:0 0 12px;font-size:15px;line-height:1.55;color:#334155;">%s</p>
                              <p style="margin:0 0 20px;font-size:14px;line-height:1.5;color:#64748b;">%s</p>
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                <tr>
                                  <td align="center" style="background-color:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid %s;border-radius:12px;padding:22px 20px;">
                                    %s
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:20px 0 0;font-size:13px;line-height:1.55;color:#64748b;">%s</p>
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
                copy.badgeBg(),
                copy.badgeColor(),
                escapeHtml(copy.badge()),
                escapeHtml(copy.intro()),
                escapeHtml(copy.lead()),
                copy.accent(),
                highlightRows,
                escapeHtml(copy.securityNote()),
                escapeHtml(copy.footer()),
                StringUtils.hasText(footerLink)
                        ? "<p style=\"margin:0;font-size:12px;line-height:1.5;color:#94a3b8;\">%s %s</p>".formatted(
                        escapeHtml(copy.footerVisit()), footerLink)
                        : ""
        );

        String plain = copy.plainBody(event, when, ip, device).trim();
        return new AlarmEmailContent(copy.subject(), html, plain);
    }

    private static String buildHighlightRows(
            LoginSecurityAlertMessage event,
            Copy copy,
            String when,
            String ip,
            String device
    ) {
        String type = event.getAlertType() == null ? "" : event.getAlertType().trim().toUpperCase();
        return switch (type) {
            case "ADMIN_MESSAGE" -> rows(
                    copy.row1Label(), displayAccount(event),
                    copy.row2Label(), safe(event.getAdminMessageBody()),
                    copy.whenField(), when,
                    copy.ipField(), "—"
            );
            case "ACCOUNT_FROZEN" -> rows(
                    copy.row1Label(), displayAccount(event),
                    copy.row2Label(), safe(event.getAdminMessageBody()),
                    copy.whenField(), when,
                    copy.ipField(), "—"
            );
            case "ACCOUNT_UNFROZEN" -> rows(
                    copy.row1Label(), displayAccount(event),
                    copy.whenField(), when,
                    copy.ipField(), "—",
                    copy.deviceField(), "—"
            );
            case "ACCOUNT_DELETED_BY_ADMIN" -> rows(
                    copy.row1Label(), displayAccount(event),
                    copy.row2Label(), deletedDetail(event),
                    copy.whenField(), when,
                    copy.ipField(), "—"
            );
            case "REGISTRATION_EMAIL_UNBLOCKED" -> rows(
                    copy.row1Label(), displayAccount(event),
                    copy.whenField(), when,
                    copy.ipField(), "—",
                    copy.deviceField(), "—"
            );
            case "USERNAME_CHANGED" -> rows(
                    copy.row1Label(), formatUsername(event.getPreviousUsername()),
                    copy.row2Label(), formatUsername(event.getNewUsername()),
                    copy.whenField(), when,
                    copy.ipField(), ip
            );
            case "EMAIL_CHANGED_OLD_ACCOUNT", "EMAIL_CHANGED_NEW_ACCOUNT" -> rows(
                    copy.row1Label(), safe(event.getPreviousEmail()),
                    copy.row2Label(), safe(event.getNewEmail()),
                    copy.row3Label(), formatUsername(event.getUsername()),
                    copy.whenField(), when
            );
            default -> rows(
                    copy.row1Label(), displayAccount(event),
                    copy.whenField(), when,
                    copy.ipField(), ip,
                    copy.deviceField(), device
            );
        };
    }

    private static String rows(String l1, String v1, String l2, String v2, String l3, String v3, String l4, String v4) {
        return """
                <p style="margin:0 0 8px;font-size:14px;line-height:1.5;color:#334155;"><strong>%s</strong> %s</p>
                <p style="margin:0 0 8px;font-size:14px;line-height:1.5;color:#334155;"><strong>%s</strong> %s</p>
                <p style="margin:0 0 8px;font-size:14px;line-height:1.5;color:#334155;"><strong>%s</strong> %s</p>
                <p style="margin:0;font-size:14px;line-height:1.5;color:#334155;"><strong>%s</strong> %s</p>
                """.formatted(
                escapeHtml(l1), escapeHtml(v1),
                escapeHtml(l2), escapeHtml(v2),
                escapeHtml(l3), escapeHtml(v3),
                escapeHtml(l4), escapeHtml(v4)
        );
    }

    private static String displayAccount(LoginSecurityAlertMessage event) {
        if (StringUtils.hasText(event.getUserEmail())) {
            return event.getUserEmail().trim();
        }
        if (StringUtils.hasText(event.getUsername())) {
            return "@" + event.getUsername().trim();
        }
        return "—";
    }

    private static String formatUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return "—";
        }
        String u = username.trim();
        return u.startsWith("@") ? u : "@" + u;
    }

    private static String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "—";
    }

    private static String deletedDetail(LoginSecurityAlertMessage event) {
        String body = event.getAdminMessageBody();
        if (body != null && body.startsWith("EMAIL_BLOCKED:")) {
            return body.substring("EMAIL_BLOCKED:".length()).trim();
        }
        return "—";
    }

    private static boolean emailBlockedOnDelete(LoginSecurityAlertMessage event) {
        String body = event.getAdminMessageBody();
        return body != null && body.startsWith("EMAIL_BLOCKED:");
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
            String subject,
            String brandName,
            String badge,
            String badgeBg,
            String badgeColor,
            String accent,
            String intro,
            String lead,
            String row1Label,
            String row2Label,
            String row3Label,
            String whenField,
            String ipField,
            String deviceField,
            String securityNote,
            String footer,
            String footerVisit,
            String unknownValue
    ) {
        String plainBody(LoginSecurityAlertMessage event, String when, String ip, String device) {
            String type = event.getAlertType() == null ? "" : event.getAlertType().trim().toUpperCase();
            return switch (type) {
                case "USERNAME_CHANGED" -> """
                        %s
                        %s
                        %s
                        %s
                        %s: %s
                        %s: %s
                        %s: %s
                        %s
                        """.formatted(
                        brandName, badge, intro, lead,
                        row1Label(), formatUsername(event.getPreviousUsername()),
                        row2Label(), formatUsername(event.getNewUsername()),
                        whenField(), when, securityNote()
                );
                case "ACCOUNT_DELETED_BY_ADMIN" -> {
                    if (emailBlockedOnDelete(event)) {
                        yield """
                        %s
                        %s
                        %s
                        %s
                        %s: %s
                        %s: %s
                        %s: %s
                        %s
                        """.formatted(
                                brandName, badge, intro, lead,
                                row1Label(), displayAccount(event),
                                row2Label(), deletedDetail(event),
                                whenField(), when,
                                securityNote()
                        );
                    }
                    yield """
                        %s
                        %s
                        %s
                        %s
                        %s: %s
                        %s: %s
                        %s
                        """.formatted(
                            brandName, badge, intro, lead,
                            row1Label(), displayAccount(event),
                            whenField(), when,
                            securityNote()
                    );
                }
                case "REGISTRATION_EMAIL_UNBLOCKED" -> """
                        %s
                        %s
                        %s
                        %s
                        %s: %s
                        %s: %s
                        %s
                        """.formatted(
                        brandName, badge, intro, lead,
                        row1Label(), displayAccount(event),
                        whenField(), when,
                        securityNote()
                );
                case "EMAIL_CHANGED_OLD_ACCOUNT", "EMAIL_CHANGED_NEW_ACCOUNT" -> """
                        %s
                        %s
                        %s
                        %s
                        %s: %s
                        %s: %s
                        %s: %s
                        %s: %s
                        %s
                        """.formatted(
                        brandName, badge, intro, lead,
                        row1Label(), safe(event.getPreviousEmail()),
                        row2Label(), safe(event.getNewEmail()),
                        row3Label(), formatUsername(event.getUsername()),
                        whenField(), when, securityNote()
                );
                default -> """
                        %s
                        %s
                        %s
                        %s
                        %s: %s
                        %s: %s
                        %s: %s
                        %s: %s
                        %s
                        """.formatted(
                        brandName, badge, intro, lead,
                        row1Label(), displayAccount(event),
                        whenField(), when,
                        ipField(), ip,
                        deviceField(), device,
                        securityNote()
                );
            };
        }

        static Copy forAlertType(String lang, String alertType) {
            return switch (alertType) {
                case "PASSWORD_CHANGED" -> passwordChanged(lang);
                case "USERNAME_CHANGED" -> usernameChanged(lang);
                case "EMAIL_CHANGED_OLD_ACCOUNT" -> emailOld(lang);
                case "EMAIL_CHANGED_NEW_ACCOUNT" -> emailNew(lang);
                case "LOGIN_FAILED" -> loginFailed(lang);
                case "ACCOUNT_FROZEN" -> accountFrozen(lang);
                case "ACCOUNT_UNFROZEN" -> accountUnfrozen(lang);
                case "ADMIN_MESSAGE" -> adminMessage(lang);
                case "ACCOUNT_DELETED_BY_ADMIN" -> accountDeletedByAdmin(lang);
                case "REGISTRATION_EMAIL_UNBLOCKED" -> registrationEmailUnblocked(lang);
                default -> loginSucceeded(lang);
            };
        }

        private static Copy accountFrozen(String lang) {
            return switch (lang) {
                case "tr" -> base(
                        "Hesabınız donduruldu — Finans Portalı",
                        "Finans Portalı", "Hesap askıya alındı",
                        "rgba(220,38,38,0.12)", "#b91c1c", "#dc2626",
                        "Merhaba,",
                        "Finans Portalı hesabınıza erişim geçici olarak durduruldu. Açık oturumlar sonlandırıldı.",
                        "Hesap", "Gerekçe", "Zaman",
                        "Bu işlemi siz talep etmediyseniz destek ekibiyle iletişime geçin.",
                        lang
                );
                case "de" -> base(
                        "Konto gesperrt — Finanzportal",
                        "Finanzportal", "Konto gesperrt",
                        "rgba(220,38,38,0.12)", "#b91c1c", "#dc2626",
                        "Hallo,",
                        "Der Zugang zu Ihrem Finanzportal-Konto wurde vorübergehend gesperrt.",
                        "Konto", "Grund", "Zeit",
                        "Wenn Sie dies nicht veranlasst haben, kontaktieren Sie den Support.",
                        lang
                );
                default -> base(
                        "Account suspended — Finance Portal",
                        "Finance Portal", "Account suspended",
                        "rgba(220,38,38,0.12)", "#b91c1c", "#dc2626",
                        "Hello,",
                        "Access to your Finance Portal account has been temporarily suspended.",
                        "Account", "Reason", "Time",
                        "If you did not request this, contact support.",
                        lang
                );
            };
        }

        private static Copy registrationEmailUnblocked(String lang) {
            return switch (lang) {
                case "tr" -> base(
                        "Kayıt engeliniz kaldırıldı — Finans Portalı",
                        "Finans Portalı", "Kayıt engeli kaldırıldı",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Merhaba,",
                        "Finans Portalı yöneticisi e-posta adresiniz için uygulanan kayıt engelini kaldırdı. Bu adresle yeniden hesap oluşturabilirsiniz.",
                        "E-posta", null, "Zaman",
                        "Bu işlemi siz talep etmediyseniz destek ekibiyle iletişime geçin.",
                        lang
                );
                case "de" -> base(
                        "Registrierungssperre aufgehoben — Finanzportal",
                        "Finanzportal", "Sperre aufgehoben",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Hallo,",
                        "Ein Administrator hat die Registrierungssperre für Ihre E-Mail-Adresse aufgehoben. Sie können damit wieder ein Konto erstellen.",
                        "E-Mail", null, "Zeit",
                        "Wenn Sie dies nicht veranlasst haben, kontaktieren Sie den Support.",
                        lang
                );
                default -> base(
                        "Registration block lifted — Finance Portal",
                        "Finance Portal", "Block lifted",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Hello,",
                        "An administrator removed the registration block on your email address. You may create a new account with it again.",
                        "Email", null, "Time",
                        "If you did not request this, contact support.",
                        lang
                );
            };
        }

        private static Copy accountDeletedByAdmin(String lang) {
            return switch (lang) {
                case "tr" -> base(
                        "Hesabınız silindi — Finans Portalı",
                        "Finans Portalı", "Hesap kaldırıldı",
                        "rgba(220,38,38,0.12)", "#b91c1c", "#dc2626",
                        "Merhaba,",
                        "Finans Portalı hesabınız bir yönetici tarafından kalıcı olarak silindi. Tüm verileriniz sistemden kaldırıldı.",
                        "Hesap", "Engellenen e-posta", "Zaman",
                        "Bu işlemi siz talep etmediyseniz destek ekibiyle iletişime geçin.",
                        lang
                );
                case "de" -> base(
                        "Konto gelöscht — Finanzportal",
                        "Finanzportal", "Konto entfernt",
                        "rgba(220,38,38,0.12)", "#b91c1c", "#dc2626",
                        "Hallo,",
                        "Ihr Finanzportal-Konto wurde von einem Administrator dauerhaft gelöscht.",
                        "Konto", "Gesperrte E-Mail", "Zeit",
                        "Wenn Sie dies nicht veranlasst haben, kontaktieren Sie den Support.",
                        lang
                );
                default -> base(
                        "Account deleted — Finance Portal",
                        "Finance Portal", "Account removed",
                        "rgba(220,38,38,0.12)", "#b91c1c", "#dc2626",
                        "Hello,",
                        "Your Finance Portal account was permanently removed by an administrator.",
                        "Account", "Blocked email", "Time",
                        "If you did not request this, contact support.",
                        lang
                );
            };
        }

        private static Copy accountUnfrozen(String lang) {
            return switch (lang) {
                case "tr" -> base(
                        "Hesabınız yeniden açıldı — Finans Portalı",
                        "Finans Portalı", "Erişim geri yüklendi",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Merhaba,",
                        "Finans Portalı hesabınıza tekrar giriş yapabilirsiniz.",
                        "Hesap", null, null,
                        "Güvenliğiniz için parolanızı düzenli aralıklarla güncelleyin.",
                        lang
                );
                case "de" -> base(
                        "Konto wieder freigeschaltet — Finanzportal",
                        "Finanzportal", "Zugang wiederhergestellt",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Hallo,",
                        "Sie können sich wieder bei Ihrem Finanzportal-Konto anmelden.",
                        "Konto", null, null,
                        "Aktualisieren Sie Ihr Passwort regelmäßig.",
                        lang
                );
                default -> base(
                        "Account restored — Finance Portal",
                        "Finance Portal", "Access restored",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Hello,",
                        "You can sign in to your Finance Portal account again.",
                        "Account", null, null,
                        "Update your password regularly for security.",
                        lang
                );
            };
        }

        private static Copy adminMessage(String lang) {
            return switch (lang) {
                case "tr" -> base(
                        "Finans Portalı — Destek mesajı",
                        "Finans Portalı", "Yönetici mesajı",
                        "rgba(245,158,11,0.15)", "#b45309", "#f59e0b",
                        "Merhaba,",
                        "Finans Portalı ekibinden size bir mesaj iletildi:",
                        "Hesap", "Mesaj", "Zaman",
                        "Bu mesaja portal bildirimlerinizden de ulaşabilirsiniz.",
                        lang
                );
                case "de" -> base(
                        "Finanzportal — Support-Nachricht",
                        "Finanzportal", "Nachricht vom Team",
                        "rgba(245,158,11,0.15)", "#b45309", "#f59e0b",
                        "Hallo,",
                        "Das Finanzportal-Team hat Ihnen eine Nachricht gesendet:",
                        "Konto", "Nachricht", "Zeit",
                        "Sie finden diese Nachricht auch in Ihren Portal-Benachrichtigungen.",
                        lang
                );
                default -> base(
                        "Finance Portal — Support message",
                        "Finance Portal", "Message from our team",
                        "rgba(245,158,11,0.15)", "#b45309", "#f59e0b",
                        "Hello,",
                        "The Finance Portal team sent you the following message:",
                        "Account", "Message", "Time",
                        "You can also read this in your portal notifications.",
                        lang
                );
            };
        }

        private static Copy loginSucceeded(String lang) {
            return switch (lang) {
                case "tr" -> base(
                        "Hesabınıza giriş yapıldı — Finans Portalı",
                        "Finans Portalı", "Başarılı giriş",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Merhaba,",
                        "Finans Portalı hesabınıza başarılı bir giriş yapıldı. Oturum özeti:",
                        "Hesap", null, null,
                        "Bu işlemi siz yapmadıysanız parolanızı hemen değiştirin.",
                        lang
                );
                case "de" -> base(
                        "Anmeldung bei Ihrem Konto — Finanzportal",
                        "Finanzportal", "Erfolgreiche Anmeldung",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Hallo,",
                        "Erfolgreiche Anmeldung bei Ihrem Finanzportal-Konto. Zusammenfassung:",
                        "Konto", null, null,
                        "Wenn Sie dies nicht waren, ändern Sie umgehend Ihr Passwort.",
                        lang
                );
                default -> base(
                        "Sign-in to your account — Finance Portal",
                        "Finance Portal", "Successful sign-in",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Hello,",
                        "A successful sign-in to your Finance Portal account was recorded.",
                        "Account", null, null,
                        "If this was not you, change your password immediately.",
                        lang
                );
            };
        }

        private static Copy loginFailed(String lang) {
            return switch (lang) {
                case "tr" -> base(
                        "Hesabınıza giriş yapılmaya çalışıldı — Finans Portalı",
                        "Finans Portalı", "Başarısız giriş denemesi",
                        "rgba(220,38,38,0.12)", "#b91c1c", "#dc2626",
                        "Merhaba,",
                        "Hesabınıza hatalı parola ile giriş yapılmaya çalışıldı. Deneme özeti:",
                        "Hesap", null, null,
                        "Bu denemeyi siz yapmadıysanız parolanızı değiştirin.",
                        lang
                );
                case "de" -> base(
                        "Anmeldeversuch — Finanzportal",
                        "Finanzportal", "Fehlgeschlagener Anmeldeversuch",
                        "rgba(220,38,38,0.12)", "#b91c1c", "#dc2626",
                        "Hallo,",
                        "Es wurde ein fehlgeschlagener Anmeldeversuch mit falschem Passwort registriert.",
                        "Konto", null, null,
                        "Wenn Sie dies nicht waren, ändern Sie Ihr Passwort.",
                        lang
                );
                default -> base(
                        "Sign-in attempt — Finance Portal",
                        "Finance Portal", "Failed sign-in attempt",
                        "rgba(220,38,38,0.12)", "#b91c1c", "#dc2626",
                        "Hello,",
                        "Someone tried to sign in with an incorrect password.",
                        "Account", null, null,
                        "If this was not you, change your password.",
                        lang
                );
            };
        }

        private static Copy passwordChanged(String lang) {
            return switch (lang) {
                case "tr" -> base(
                        "Parolanız değiştirildi — Finans Portalı",
                        "Finans Portalı", "Parola güncellendi",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Merhaba,",
                        "Finans Portalı hesabınızın parolası başarıyla değiştirildi. İşlem özeti:",
                        "Hesap", null, null,
                        "Bu değişikliği siz yapmadıysanız derhal destek ile iletişime geçin.",
                        lang
                );
                case "de" -> base(
                        "Passwort geändert — Finanzportal",
                        "Finanzportal", "Passwort aktualisiert",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Hallo,",
                        "Das Passwort Ihres Finanzportal-Kontos wurde erfolgreich geändert.",
                        "Konto", null, null,
                        "Wenn Sie dies nicht waren, kontaktieren Sie umgehend den Support.",
                        lang
                );
                default -> base(
                        "Password changed — Finance Portal",
                        "Finance Portal", "Password updated",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Hello,",
                        "Your Finance Portal account password was changed successfully.",
                        "Account", null, null,
                        "If you did not make this change, contact support immediately.",
                        lang
                );
            };
        }

        private static Copy usernameChanged(String lang) {
            return switch (lang) {
                case "tr" -> base(
                        "Kullanıcı adınız değiştirildi — Finans Portalı",
                        "Finans Portalı", "Kullanıcı adı güncellendi",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Merhaba,",
                        "Portal hesabınızın kullanıcı adı güncellendi. Bundan sonra giriş için yeni kullanıcı adını kullanın:",
                        "Önceki kullanıcı adı", "Yeni kullanıcı adı", null,
                        "Bu değişikliği siz yapmadıysanız derhal destek ile iletişime geçin.",
                        lang
                );
                case "de" -> base(
                        "Benutzername geändert — Finanzportal",
                        "Finanzportal", "Benutzername aktualisiert",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Hallo,",
                        "Der Benutzername Ihres Portal-Kontos wurde geändert. Verwenden Sie ab jetzt den neuen Namen:",
                        "Vorheriger Benutzername", "Neuer Benutzername", null,
                        "Wenn Sie dies nicht waren, kontaktieren Sie umgehend den Support.",
                        lang
                );
                default -> base(
                        "Username changed — Finance Portal",
                        "Finance Portal", "Username updated",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Hello,",
                        "Your portal username was updated. Use the new username to sign in:",
                        "Previous username", "New username", null,
                        "If you did not make this change, contact support immediately.",
                        lang
                );
            };
        }

        private static Copy emailOld(String lang) {
            return switch (lang) {
                case "tr" -> base(
                        "E-posta adresiniz değiştirildi — görüşürüz",
                        "Finans Portalı", "E-posta adresi kaldırıldı",
                        "rgba(245,158,11,0.15)", "#b45309", "#f59e0b",
                        "Merhaba,",
                        "Finans Portalı hesabınızda kayıtlı bu e-posta adresi değiştirildi. Bu adres artık hesabınıza bağlı değil — görüşürüz.",
                        "Önceki e-posta", "Yeni e-posta", "Kullanıcı adı",
                        "Bu değişikliği siz yapmadıysanız derhal destek ile iletişime geçin ve parolanızı değiştirin.",
                        lang
                );
                case "de" -> base(
                        "E-Mail-Adresse geändert — auf Wiedersehen",
                        "Finanzportal", "E-Mail entfernt",
                        "rgba(245,158,11,0.15)", "#b45309", "#f59e0b",
                        "Hallo,",
                        "Diese E-Mail-Adresse ist nicht mehr mit Ihrem Finanzportal-Konto verknüpft — auf Wiedersehen.",
                        "Vorherige E-Mail", "Neue E-Mail", "Benutzername",
                        "Wenn Sie dies nicht waren, kontaktieren Sie umgehend den Support.",
                        lang
                );
                default -> base(
                        "Your email was changed — goodbye",
                        "Finance Portal", "Email removed from account",
                        "rgba(245,158,11,0.15)", "#b45309", "#f59e0b",
                        "Hello,",
                        "This email address is no longer linked to your Finance Portal account — goodbye.",
                        "Previous email", "New email", "Username",
                        "If you did not make this change, contact support immediately.",
                        lang
                );
            };
        }

        private static Copy emailNew(String lang) {
            return switch (lang) {
                case "tr" -> base(
                        "E-posta adresiniz güncellendi — hoş geldiniz",
                        "Finans Portalı", "Yeni e-posta doğrulandı",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Merhaba,",
                        "Finans Portalı hesabınızın yeni e-posta adresi doğrulandı ve hesabınıza bağlandı. Hoş geldiniz!",
                        "Önceki e-posta", "Yeni e-posta", "Kullanıcı adı",
                        "Bundan sonra giriş ve bildirimler bu adrese gönderilir.",
                        lang
                );
                case "de" -> base(
                        "E-Mail aktualisiert — willkommen",
                        "Finanzportal", "Neue E-Mail bestätigt",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Hallo,",
                        "Ihre neue E-Mail-Adresse wurde bestätigt und mit Ihrem Konto verknüpft. Willkommen!",
                        "Vorherige E-Mail", "Neue E-Mail", "Benutzername",
                        "Anmeldung und Benachrichtigungen verwenden ab jetzt diese Adresse.",
                        lang
                );
                default -> base(
                        "Email updated — welcome",
                        "Finance Portal", "New email confirmed",
                        "rgba(22,163,74,0.12)", "#15803d", "#16a34a",
                        "Hello,",
                        "Your new email address was verified and linked to your account. Welcome!",
                        "Previous email", "New email", "Username",
                        "Sign-in and notifications will use this address from now on.",
                        lang
                );
            };
        }

        private static Copy base(
                String subject,
                String brandName,
                String badge,
                String badgeBg,
                String badgeColor,
                String accent,
                String intro,
                String lead,
                String row1,
                String row2,
                String row3,
                String securityNote,
                String lang
        ) {
            String footer = switch (lang) {
                case "tr" -> "Bu mesaj hesap güvenliği bildirimidir; yatırım tavsiyesi değildir.";
                case "de" -> "Diese Nachricht ist eine Sicherheitsbenachrichtigung, keine Anlageberatung.";
                default -> "This is a security notification, not investment advice.";
            };
            String footerVisit = switch (lang) {
                case "tr" -> "Portal:";
                case "de" -> "Portal:";
                default -> "Visit:";
            };
            String unknown = switch (lang) {
                case "tr" -> "Bilinmiyor";
                case "de" -> "Unbekannt";
                default -> "Unknown";
            };
            return new Copy(
                    subject,
                    brandName,
                    badge,
                    badgeBg,
                    badgeColor,
                    accent,
                    intro,
                    lead,
                    row1,
                    row2,
                    row3,
                    switch (lang) {
                        case "tr" -> "Zaman";
                        case "de" -> "Zeitpunkt";
                        default -> "Time";
                    },
                    switch (lang) {
                        case "tr" -> "IP adresi";
                        case "de" -> "IP-Adresse";
                        default -> "IP address";
                    },
                    switch (lang) {
                        case "tr" -> "Cihaz / tarayıcı";
                        case "de" -> "Gerät / Browser";
                        default -> "Device / browser";
                    },
                    securityNote,
                    footer,
                    footerVisit,
                    unknown
            );
        }
    }
}
