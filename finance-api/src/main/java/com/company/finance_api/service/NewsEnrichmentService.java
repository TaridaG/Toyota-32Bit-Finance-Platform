package com.company.finance_api.service;

import com.company.finance_api.dto.NewsEnrichedDetailResponse;
import com.company.finance_api.dto.NewsEnrichedPageResponse;
import com.company.finance_api.dto.NewsEnrichedResponse;
import com.company.finance_api.dto.NewsOriginalResponse;
import java.time.Instant;
import java.util.List;

/** NewsEnrichmentService iş mantığını uygular (news enrichment service). */
public interface NewsEnrichmentService {
  NewsEnrichedPageResponse getEnrichedNews(
      int page,
      int size,
      String language,
      String category,
      String sentiment,
      Integer maxAgeMinutes,
      String search);

  /** getOriginalNews sözleşmesi. */
  NewsOriginalResponse getOriginalNews(Long id);

  /** getEnrichedNewsDetail sözleşmesi. */
  NewsEnrichedDetailResponse getEnrichedNewsDetail(Long id, String language);

  List<NewsEnrichedResponse> getEnrichedChartNews(
      String symbol,
      String categoryUi,
      Instant fromInclusive,
      Instant toInclusive,
      String language);

  NewsEnrichedPageResponse getEnrichedFavoriteNews(
      int page, int size, String language, String category, Integer maxAgeMinutes, String search);
}
