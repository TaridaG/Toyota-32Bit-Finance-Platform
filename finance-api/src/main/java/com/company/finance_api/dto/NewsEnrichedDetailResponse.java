package com.company.finance_api.dto;

import java.time.Instant;
import java.util.List;

/** NewsEnrichedDetailResponse — API transfer nesnesi (DTO/response/request). */
public record NewsEnrichedDetailResponse(
    Long id,
    String title,
    String summary,
    String titleOriginal,
    String summaryOriginal,
    String translatedLanguage,
    boolean translated,
    String articleUrl,
    String imageUrl,
    String sourceName,
    String category,
    String categoryUi,
    Instant publishedAt,
    String sentiment,
    List<String> relatedSymbols,
    List<String> topicTags,
    List<NewsRelatedAssetPerformance> relatedAssets) {}
