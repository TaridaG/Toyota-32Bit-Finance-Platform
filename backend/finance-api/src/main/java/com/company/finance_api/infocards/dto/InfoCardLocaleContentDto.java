package com.company.finance_api.infocards.dto;

import java.util.List;

public record InfoCardLocaleContentDto(
        String title,
        String shortDescription,
        String detailedDescription,
        String howToInterpret,
        String commonMistake,
        String example,
        List<String> relatedTerms
) {
}
