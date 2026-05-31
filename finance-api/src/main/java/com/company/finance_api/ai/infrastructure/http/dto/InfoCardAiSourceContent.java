package com.company.finance_api.ai.infrastructure.http.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Çeviri için kaynak locale info-card içeriği. */
public record InfoCardAiSourceContent(
    @NotBlank String title,
    @NotBlank String shortDescription,
    String detailedDescription,
    String howToInterpret,
    String commonMistake,
    String example,
    List<String> relatedTerms) {}
