package com.company.finance_api.infocards.application;

import com.company.finance_api.infocards.domain.InfoCardEntity;
import com.company.finance_api.infocards.infrastructure.http.dto.InfoCardLocaleContentDto;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Info-card locale kodlarını normalize eder ve çeviri fallback uygular. */
public final class InfoCardLocaleResolver {

  public static final List<String> SUPPORTED_LOCALES = List.of("tr", "en", "de");

  private InfoCardLocaleResolver() {}

  /** Geçerli locale kodunu döner; boşsa varsayılan kullanır. */
  public static String normalizeLocale(String raw) {
    if (raw == null || raw.isBlank()) {
      return "en";
    }
    String language = raw.toLowerCase(Locale.ROOT).replace('_', '-').split("-")[0];
    return SUPPORTED_LOCALES.contains(language) ? language : "en";
  }

  /** Entity'den istenen locale içeriğini çözümler. */
  public static InfoCardLocaleContentDto resolve(InfoCardEntity entity, String locale) {
    String normalized = normalizeLocale(locale);
    Map<String, InfoCardLocaleContentDto> translations = entity.getTranslations();
    if (translations != null && !translations.isEmpty()) {
      InfoCardLocaleContentDto exact = translations.get(normalized);
      if (exact != null && hasText(exact.title())) {
        return exact;
      }
      for (String fallback : SUPPORTED_LOCALES) {
        InfoCardLocaleContentDto candidate = translations.get(fallback);
        if (candidate != null && hasText(candidate.title())) {
          return candidate;
        }
      }
    }
    return new InfoCardLocaleContentDto(
        entity.getTitle(),
        entity.getShortDescription(),
        entity.getDetailedDescription(),
        entity.getHowToInterpret(),
        entity.getCommonMistake(),
        entity.getExampleText(),
        List.copyOf(entity.getRelatedTerms()));
  }

  /** Çeviri haritasından birincil locale içeriğini seçer. */
  public static InfoCardLocaleContentDto pickPrimary(
      Map<String, InfoCardLocaleContentDto> translations) {
    if (translations == null || translations.isEmpty()) {
      return null;
    }
    for (String locale : SUPPORTED_LOCALES) {
      InfoCardLocaleContentDto content = translations.get(locale);
      if (content != null && hasText(content.title()) && hasText(content.shortDescription())) {
        return content;
      }
    }
    return translations.values().stream()
        .filter(c -> c != null && hasText(c.title()))
        .findFirst()
        .orElse(null);
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
