package com.company.finance_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PortalUpdatePreferencesRequest — API transfer nesnesi (DTO/response/request). */
public class PortalUpdatePreferencesRequest {

  @NotBlank
  @Size(max = 8)
  private String preferredLocale;

  @NotBlank
  @Size(max = 8)
  private String preferredCurrency;

  public String getPreferredLocale() {
    return preferredLocale;
  }

  public void setPreferredLocale(String preferredLocale) {
    this.preferredLocale = preferredLocale;
  }

  public String getPreferredCurrency() {
    return preferredCurrency;
  }

  public void setPreferredCurrency(String preferredCurrency) {
    this.preferredCurrency = preferredCurrency;
  }
}
