package com.company.finance_api.auth.domain;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/** Giriş denemesi bağlamı: tercih edilen locale, client IP ve User-Agent. */
public record LoginAttemptContext(String preferredLocale, String clientIp, String userAgent) {

  /** HTTP header ve request bilgisinden bağlam oluşturur. */
  public static LoginAttemptContext from(
      String preferredLocale, String forwardedFor, String userAgent, HttpServletRequest request) {
    return new LoginAttemptContext(
        preferredLocale, resolveClientIp(forwardedFor, request), sanitizeUserAgent(userAgent));
  }

  private static String resolveClientIp(String forwardedFor, HttpServletRequest request) {
    if (StringUtils.hasText(forwardedFor)) {
      String first = forwardedFor.split(",")[0].trim();
      if (StringUtils.hasText(first)) {
        return first;
      }
    }
    if (request != null && StringUtils.hasText(request.getRemoteAddr())) {
      return request.getRemoteAddr().trim();
    }
    return null;
  }

  private static String sanitizeUserAgent(String userAgent) {
    if (!StringUtils.hasText(userAgent)) {
      return null;
    }
    String trimmed = userAgent.trim();
    return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
  }
}
