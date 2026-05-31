package com.company.finance_api.mfa.application;

import com.company.finance_api.bootstrap.config.MfaProperties;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** TOTP secret üretimi, kod doğrulama ve QR oluşturma. */
@Service
public class PortalTotpService {

  private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
  private final CodeVerifier codeVerifier;
  private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
  private final MfaProperties properties;

  public PortalTotpService(MfaProperties properties) {
    this.properties = properties;
    TimeProvider timeProvider = new SystemTimeProvider();
    CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
    this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
  }

  /** Yeni Base32 TOTP secret üretir. */
  public String generateSecret() {
    return secretGenerator.generate();
  }

  /** 6 haneli TOTP kodunu doğrular. */
  public boolean verifyCode(String base32Secret, String code) {
    if (!StringUtils.hasText(base32Secret) || !StringUtils.hasText(code)) {
      return false;
    }
    String normalized = code.replaceAll("\\s+", "");
    if (!normalized.matches("\\d{6}")) {
      return false;
    }
    return codeVerifier.isValidCode(base32Secret.trim(), normalized);
  }

  /** Authenticator uygulaması için QR veri modeli oluşturur. */
  public QrData buildQrData(String accountLabel, String base32Secret) {
    String label = accountLabel != null ? accountLabel.trim() : "user";
    return new QrData.Builder()
        .label(label)
        .secret(base32Secret)
        .issuer(properties.getIssuer())
        .algorithm(HashingAlgorithm.SHA1)
        .digits(6)
        .period(30)
        .build();
  }

  /** otpauth:// URI döner. */
  public String buildOtpAuthUri(QrData data) {
    return data.getUri();
  }

  /** QR kodunu PNG bayt dizisi olarak üretir. */
  public byte[] generateQrPng(QrData data) {
    try {
      return qrGenerator.generate(data);
    } catch (QrGenerationException ex) {
      throw new IllegalStateException("QR generation failed", ex);
    }
  }
}
