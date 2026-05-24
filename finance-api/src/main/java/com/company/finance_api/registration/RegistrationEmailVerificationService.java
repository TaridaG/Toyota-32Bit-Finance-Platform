package com.company.finance_api.registration;

import com.company.finance_api.bootstrap.config.RegistrationVerificationProperties;
import com.company.finance_api.dto.PublicSendVerificationCodeResponse;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Kayıt/profil e-posta doğrulama kodu üretimi, gönderimi ve doğrulaması. */
@Service
public class RegistrationEmailVerificationService {

  private static final String LOGO_RESOURCE = "email/site-logo.png";
  private static final String LOGO_CONTENT_ID = "portalLogo";

  private final EmailVerificationCodeRepository repository;
  private final JavaMailSender mailSender;
  private final RegistrationVerificationProperties properties;
  private final VerificationEmailTemplateService emailTemplateService;
  private final VerificationEmailLocaleResolver localeResolver;
  private final RegistrationEmailAvailabilityService registrationEmailAvailabilityService;
  private final SecureRandom secureRandom = new SecureRandom();

  public RegistrationEmailVerificationService(
      EmailVerificationCodeRepository repository,
      JavaMailSender mailSender,
      RegistrationVerificationProperties properties,
      VerificationEmailTemplateService emailTemplateService,
      VerificationEmailLocaleResolver localeResolver,
      RegistrationEmailAvailabilityService registrationEmailAvailabilityService) {
    this.repository = repository;
    this.mailSender = mailSender;
    this.properties = properties;
    this.emailTemplateService = emailTemplateService;
    this.localeResolver = localeResolver;
    this.registrationEmailAvailabilityService = registrationEmailAvailabilityService;
  }

  /** E-posta adresine doğrulama kodu gönderir. */
  @Transactional
  public PublicSendVerificationCodeResponse sendCode(String rawEmail) {
    return sendCode(rawEmail, null, null);
  }

  /** Locale ipucu ile doğrulama kodu gönderir. */
  @Transactional
  public PublicSendVerificationCodeResponse sendCode(String rawEmail, String localeHint) {
    return sendCode(rawEmail, localeHint, null);
  }

  /** Profil değişikliğinde mevcut kullanıcıyı hariç tutarak doğrulama kodu gönderir. */
  @Transactional
  public PublicSendVerificationCodeResponse sendCode(
      String rawEmail, String localeHint, UUID excludeUserId) {
    if (!properties.isEnabled()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Email verification is disabled");
    }
    String email = normalizeEmail(rawEmail);
    if (excludeUserId == null) {
      registrationEmailAvailabilityService.assertAvailableForRegistration(email);
    } else {
      registrationEmailAvailabilityService.assertAvailableForProfileChange(email, excludeUserId);
    }
    Instant now = Instant.now();
    EmailVerificationCodeEntry entry = repository.findById(email).orElse(null);
    if (entry != null
        && entry.getResendAvailableAt() != null
        && now.isBefore(entry.getResendAvailableAt())) {
      long remaining = entry.getResendAvailableAt().getEpochSecond() - now.getEpochSecond();
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS,
          "Please wait " + Math.max(remaining, 1) + " seconds before requesting another code");
    }

    String code = generateCode(properties.getCodeLength());
    Instant expiresAt = now.plusSeconds(properties.getTtlSeconds());
    Instant resendAt = now.plusSeconds(properties.getResendCooldownSeconds());

    EmailVerificationCodeEntry row = entry == null ? new EmailVerificationCodeEntry() : entry;
    row.setEmail(email);
    row.setCodeHash(hash(email, code));
    row.setExpiresAt(expiresAt);
    row.setResendAvailableAt(resendAt);
    row.setAttempts(0);
    row.setUpdatedAt(now);
    if (row.getCreatedAt() == null) {
      row.setCreatedAt(now);
    }
    repository.save(row);

    String locale = localeResolver.resolve(localeHint, email);
    sendEmail(email, code, properties.getTtlSeconds(), locale);
    return new PublicSendVerificationCodeResponse(
        properties.getTtlSeconds(), properties.getResendCooldownSeconds());
  }

  /** Gönderilen kodu doğrular; geçersizse HTTP exception fırlatır. */
  @Transactional
  public void verifyCodeOrThrow(String rawEmail, String rawCode) {
    if (!properties.isEnabled()) {
      return;
    }
    String email = normalizeEmail(rawEmail);
    String code = normalizeCode(rawCode);
    EmailVerificationCodeEntry row =
        repository
            .findById(email)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Verification code not found"));
    Instant now = Instant.now();
    if (now.isAfter(row.getExpiresAt())) {
      repository.delete(row);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code expired");
    }
    if (row.getAttempts() >= properties.getMaxAttempts()) {
      repository.delete(row);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification attempts exceeded");
    }
    String expectedHash = hash(email, code);
    if (!expectedHash.equals(row.getCodeHash())) {
      row.setAttempts(row.getAttempts() + 1);
      row.setUpdatedAt(now);
      repository.save(row);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code is invalid");
    }
    repository.delete(row);
  }

  private void sendEmail(String to, String code, int ttlSeconds, String locale) {
    VerificationEmailContent content = emailTemplateService.build(locale, code, ttlSeconds);
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
      helper.setTo(to);
      if (properties.getFrom() != null && !properties.getFrom().isBlank()) {
        helper.setFrom(properties.getFrom());
      }
      helper.setSubject(content.subject());
      helper.setText(content.plainBody(), content.htmlBody());
      ClassPathResource logo = new ClassPathResource(LOGO_RESOURCE);
      if (logo.exists()) {
        helper.addInline(LOGO_CONTENT_ID, logo, "image/png");
      }
      mailSender.send(mimeMessage);
    } catch (Exception ex) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Unable to send verification email", ex);
    }
  }

  private String generateCode(int length) {
    int safeLength = Math.max(4, length);
    StringBuilder out = new StringBuilder(safeLength);
    for (int i = 0; i < safeLength; i++) {
      out.append(secureRandom.nextInt(10));
    }
    return out.toString();
  }

  private String hash(String email, String code) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String input = email + ":" + code + ":" + properties.getHashSecret();
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to hash verification code", ex);
    }
  }

  private static String normalizeEmail(String value) {
    if (value == null || value.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
    }
    return value.trim().toLowerCase(Locale.ROOT);
  }

  private static String normalizeCode(String value) {
    if (value == null || value.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verificationCode is required");
    }
    return value.trim();
  }
}
