package com.company.finance_api.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Portal self-service kayıt endpoint'inin açık/kapalı durumu ({@code app.registration.enabled}).
 */
@ConfigurationProperties(prefix = "app.registration")
public class RegistrationProperties {

  /** {@code false} iken {@code POST /api/public/register} 503 döner. */
  private boolean enabled = true;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
