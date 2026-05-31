package com.company.finance_api.auth.domain;

/** Login güvenlik e-postalarında kullanılan alert türleri. */
public enum LoginSecurityAlertType {
  LOGIN_SUCCEEDED,
  LOGIN_FAILED,
  PASSWORD_CHANGED,
  USERNAME_CHANGED,
  EMAIL_CHANGED_OLD_ACCOUNT,
  EMAIL_CHANGED_NEW_ACCOUNT,
  ACCOUNT_FROZEN,
  ACCOUNT_UNFROZEN,
  ADMIN_MESSAGE,
  ACCOUNT_DELETED_BY_ADMIN,
  REGISTRATION_EMAIL_UNBLOCKED
}
