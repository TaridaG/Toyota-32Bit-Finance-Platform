package com.company.finance_api.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Trusted device cookie adı, ömrü ve imza secret'ı ({@code app.auth.trusted-device}). */
@ConfigurationProperties(prefix = "app.auth.trusted-device")
public class TrustedDeviceProperties {

  private boolean enabled = true;

  private String cookieName = "finance_trusted_device";

  private int maxAgeDays = 30;

  private String signingSecret = "change-me-trusted-device";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getCookieName() {
    return cookieName;
  }

  public void setCookieName(String cookieName) {
    this.cookieName = cookieName;
  }

  public int getMaxAgeDays() {
    return maxAgeDays;
  }

  public void setMaxAgeDays(int maxAgeDays) {
    this.maxAgeDays = maxAgeDays;
  }

  public String getSigningSecret() {
    return signingSecret;
  }

  public void setSigningSecret(String signingSecret) {
    this.signingSecret = signingSecret;
  }
}
