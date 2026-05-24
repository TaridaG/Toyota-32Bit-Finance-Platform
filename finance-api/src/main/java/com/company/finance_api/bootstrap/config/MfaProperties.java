package com.company.finance_api.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MFA challenge şifreleme, issuer adı ve doğrulama limitleri için {@code app.mfa} config binding'i.
 */
@ConfigurationProperties(prefix = "app.mfa")
public class MfaProperties {

  /** AES key material (herhangi uzunluk; SHA-256 türetilir). Restart'lar arasında sabit kalmalı. */
  private String encryptionSecret = "change-me-mfa-encryption";

  private String issuer = "Finance Portal";

  private int challengeTtlSeconds = 300;

  private int maxVerifyAttempts = 5;

  public String getEncryptionSecret() {
    return encryptionSecret;
  }

  public void setEncryptionSecret(String encryptionSecret) {
    this.encryptionSecret = encryptionSecret;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public int getChallengeTtlSeconds() {
    return challengeTtlSeconds;
  }

  public void setChallengeTtlSeconds(int challengeTtlSeconds) {
    this.challengeTtlSeconds = challengeTtlSeconds;
  }

  public int getMaxVerifyAttempts() {
    return maxVerifyAttempts;
  }

  public void setMaxVerifyAttempts(int maxVerifyAttempts) {
    this.maxVerifyAttempts = maxVerifyAttempts;
  }
}
