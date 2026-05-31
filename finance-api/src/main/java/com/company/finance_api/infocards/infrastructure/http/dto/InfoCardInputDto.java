package com.company.finance_api.infocards.infrastructure.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/** Info-card oluşturma/güncelleme giriş DTO'su. */
public record InfoCardInputDto(
    String id,
    String slug,
    String title,
    List<String> targetTerms,
    List<String> targetElementIds,
    List<String> targetInstrumentSymbols,
    @NotEmpty List<String> pages,
    @NotBlank String category,
    @NotBlank String type,
    @NotBlank String difficulty,
    @NotBlank String status,
    String shortDescription,
    String detailedDescription,
    String howToInterpret,
    String commonMistake,
    String example,
    List<String> relatedTerms,
    Boolean adminOnly,
    Map<String, InfoCardLocaleContentDto> translations) {}
