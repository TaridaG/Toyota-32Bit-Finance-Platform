package com.company.finance_api.infocards.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Portal veya admin için info-card DTO'su. */
public record InfoCardDto(
    String id,
    String title,
    String slug,
    List<String> targetTerms,
    List<String> targetElementIds,
    List<String> targetInstrumentSymbols,
    List<String> pages,
    String category,
    String type,
    String difficulty,
    String status,
    String shortDescription,
    String detailedDescription,
    String howToInterpret,
    String commonMistake,
    String example,
    List<String> relatedTerms,
    Boolean adminOnly,
    Map<String, InfoCardLocaleContentDto> translations,
    Instant createdAt,
    Instant updatedAt) {}
