package com.company.finance_api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record NewsEnrichedResponse(
        Long id,
        String title,
        String summary,
        String sourceName,
        String category,
        Instant publishedAt,
        String sentiment,
        List<String> relatedSymbols,
        BigDecimal reactionPercent1h
) {
}
