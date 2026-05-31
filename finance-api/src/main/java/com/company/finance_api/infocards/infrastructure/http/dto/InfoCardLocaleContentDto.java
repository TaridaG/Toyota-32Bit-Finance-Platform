package com.company.finance_api.infocards.infrastructure.http.dto;

import java.util.List;

/** Tek locale için info-card metin alanları. */
public record InfoCardLocaleContentDto(
    String title,
    String shortDescription,
    String detailedDescription,
    String howToInterpret,
    String commonMistake,
    String example,
    List<String> relatedTerms) {}
