package com.company.finance_api.profile.infrastructure.http.dto;

import jakarta.validation.constraints.NotNull;

/** PortalUpdateNotificationsRequest — API transfer nesnesi (DTO/response/request). */
public class PortalUpdateNotificationsRequest {

  @NotNull private Boolean notifySecurityAlerts;

  @NotNull private Boolean notifyWatchlistAlerts;

  @NotNull private Boolean notifyAlarmAlerts;

  public Boolean getNotifySecurityAlerts() {
    return notifySecurityAlerts;
  }

  public void setNotifySecurityAlerts(Boolean notifySecurityAlerts) {
    this.notifySecurityAlerts = notifySecurityAlerts;
  }

  public Boolean getNotifyWatchlistAlerts() {
    return notifyWatchlistAlerts;
  }

  public void setNotifyWatchlistAlerts(Boolean notifyWatchlistAlerts) {
    this.notifyWatchlistAlerts = notifyWatchlistAlerts;
  }

  public Boolean getNotifyAlarmAlerts() {
    return notifyAlarmAlerts;
  }

  public void setNotifyAlarmAlerts(Boolean notifyAlarmAlerts) {
    this.notifyAlarmAlerts = notifyAlarmAlerts;
  }
}
