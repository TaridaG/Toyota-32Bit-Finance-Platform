package com.company.finance_api.registration;

import com.company.finance_api.bootstrap.config.RegistrationVerificationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Doğrulama e-postası HTML/plain şablonlarını locale'e göre üretir. */
@Service
public class VerificationEmailTemplateService {

  private final RegistrationVerificationProperties properties;

  public VerificationEmailTemplateService(RegistrationVerificationProperties properties) {
    this.properties = properties;
  }

  /** Verilen locale ve kod ile e-posta içeriğini oluşturur. */
  public VerificationEmailContent build(String locale, String verificationCode, int ttlSeconds) {
    String lang = VerificationMailLocale.normalize(locale);
    Copy copy = Copy.forLocale(lang);
    int ttlMinutes = Math.max(1, (int) Math.ceil(ttlSeconds / 60.0));
    String portalUrl = properties.getPortalPublicUrl();
    String footerLink =
        StringUtils.hasText(portalUrl)
            ? "<a href=\"%s\" style=\"color:#f59e0b;text-decoration:none;font-weight:600;\">%s</a>"
                .formatted(escapeHtml(portalUrl), escapeHtml(portalUrl))
            : "";

    String html =
        """
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
                              <img src="cid:portalLogo" alt="%s" width="56" height="56" style="display:block;margin:0 auto 14px;border-radius:14px;border:2px solid rgba(245,158,11,0.45);"/>
                              <p style="margin:0;font-size:22px;font-weight:700;color:#f8fafc;letter-spacing:0.02em;">%s</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px 28px 24px;">
                              <p style="margin:0 0 12px;font-size:15px;line-height:1.55;color:#334155;">%s</p>
                              <p style="margin:0 0 20px;font-size:14px;line-height:1.5;color:#64748b;">%s</p>
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                <tr>
                                  <td align="center" style="background-color:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:22px 16px;">
                                    <span style="display:inline-block;font-size:34px;font-weight:700;letter-spacing:0.35em;color:#0f172a;font-family:ui-monospace,'Cascadia Code','Segoe UI Mono',monospace;">%s</span>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:20px 0 0;font-size:13px;line-height:1.5;color:#64748b;text-align:center;">%s</p>
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
                """
            .formatted(
                lang,
                escapeHtml(copy.subject()),
                escapeHtml(copy.brandName()),
                escapeHtml(copy.brandName()),
                escapeHtml(copy.intro()),
                escapeHtml(copy.instruction()),
                escapeHtml(verificationCode),
                escapeHtml(copy.expiry(ttlMinutes)),
                escapeHtml(copy.footerIgnore()),
                StringUtils.hasText(footerLink)
                    ? "<p style=\"margin:0;font-size:12px;line-height:1.5;color:#94a3b8;\">%s %s</p>"
                        .formatted(escapeHtml(copy.footerVisit()), footerLink)
                    : "");

    String plain =
        """
                %s

                %s

                %s

                %s

                %s
                """
            .formatted(
                copy.brandName(),
                copy.intro(),
                copy.instruction(),
                verificationCode,
                copy.expiry(ttlMinutes),
                copy.footerIgnore());

    return new VerificationEmailContent(copy.subject(), html, plain.trim());
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
      String footerIgnore,
      String footerVisit) {
    static Copy forLocale(String lang) {
      String normalized = VerificationMailLocale.normalize(lang);
      return switch (normalized) {
        case "tr" ->
            new Copy(
                normalized,
                "Finans Portalı doğrulama kodunuz",
                "Finans Portalı",
                "Hesabınız için bir doğrulama kodu talep ettiniz.",
                "İşlemi tamamlamak için aşağıdaki kodu girin:",
                "Bu kodu siz istemediyseniz bu e-postayı yok sayabilirsiniz.",
                "Portal:");
        case "de" ->
            new Copy(
                normalized,
                "Ihr Finanzportal-Bestätigungscode",
                "Finanzportal",
                "Sie haben einen Bestätigungscode für Ihr Konto angefordert.",
                "Geben Sie den folgenden Code ein, um fortzufahren:",
                "Wenn Sie diesen Code nicht angefordert haben, ignorieren Sie diese E-Mail.",
                "Portal:");
        default ->
            new Copy(
                "en",
                "Your Finance Portal verification code",
                "Finance Portal",
                "You requested a verification code for your account.",
                "Enter the code below to continue:",
                "If you did not request this code, you can safely ignore this email.",
                "Visit:");
      };
    }

    String expiry(int ttlMinutes) {
      return switch (lang) {
        case "tr" -> "Bu kod yaklaşık " + ttlMinutes + " dakika geçerlidir.";
        case "de" -> "Dieser Code ist etwa " + ttlMinutes + " Minuten gültig.";
        default -> "This code is valid for about " + ttlMinutes + " minutes.";
      };
    }
  }
}
