package com.company.finance_api.ai.infrastructure.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** AI tarafından üretilen info-card locale içerik yanıtı. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InfoCardAiContentResponse(
    String title,
    String shortDescription,
    String detailedDescription,
    String howToInterpret,
    String commonMistake,
    String example,
    List<String> relatedTerms) {}
