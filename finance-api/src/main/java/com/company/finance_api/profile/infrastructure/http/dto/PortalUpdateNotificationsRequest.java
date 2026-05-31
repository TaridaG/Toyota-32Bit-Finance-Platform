package com.company.finance_api.profile.infrastructure.http.dto;

import jakarta.validation.constraints.NotNull;

/** PortalUpdateNotificationsRequest — API transfer nesnesi (DTO/response/request). */
public class PortalUpdateNotificationsRequest {

  @NotNull private Boolean notifySecurityAlerts;

  @NotNull private Boolean notifyProductUpdates;

  public Boolean getNotifySecurityAlerts() {
    return notifySecurityAlerts;
  }

  public void setNotifySecurityAlerts(Boolean notifySecurityAlerts) {
    this.notifySecurityAlerts = notifySecurityAlerts;
  }

  public Boolean getNotifyProductUpdates() {
    return notifyProductUpdates;
  }

  public void setNotifyProductUpdates(Boolean notifyProductUpdates) {
    this.notifyProductUpdates = notifyProductUpdates;
  }
}
