package com.company.finance_api.dto;

import java.time.Instant;
import java.util.List;

public record NewsEnrichedDetailResponse(
        Long id,
        String title,
        String summary,
        String titleOriginal,
        String summaryOriginal,
        String translatedLanguage,
        boolean translated,
        String articleUrl,
        String sourceName,
        String category,
        String categoryUi,
        Instant publishedAt,
        String sentiment,
        List<String> relatedSymbols,
        List<String> topicTags,
        List<NewsRelatedAssetPerformance> relatedAssets
) {
}
