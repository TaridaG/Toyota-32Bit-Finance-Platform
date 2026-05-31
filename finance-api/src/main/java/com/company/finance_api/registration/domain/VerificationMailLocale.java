package com.company.finance_api.registration.domain;

import java.util.Locale;
import org.springframework.util.StringUtils;

/** Desteklenen doğrulama mail locale kodlarını normalize eder (tr, de, en). */
public final class VerificationMailLocale {

  private VerificationMailLocale() {}

  /** Ham locale değerini desteklenen kısa koda indirger. */
  public static String normalize(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "en";
    }
    String value = raw.trim().toLowerCase(Locale.ROOT);
    if (value.contains("-")) {
      value = value.substring(0, value.indexOf('-'));
    }
    if (value.contains("_")) {
      value = value.substring(0, value.indexOf('_'));
    }
    return switch (value) {
      case "tr", "de" -> value;
      default -> "en";
    };
  }
}
