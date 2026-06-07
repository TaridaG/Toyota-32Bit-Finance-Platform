package com.company.finance_api.news.application;

import com.company.finance_api.news.infrastructure.http.dto.NewsEnrichedDetailResponse;
import com.company.finance_api.news.infrastructure.http.dto.NewsEnrichedPageResponse;
import com.company.finance_api.news.infrastructure.http.dto.NewsEnrichedResponse;
import com.company.finance_api.news.infrastructure.http.dto.NewsOriginalResponse;
import com.company.finance_api.news.infrastructure.http.dto.NewsWeeklySummaryResponse;
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
      String search,
      String relatedSymbols,
      String sourceName,
      String assetKey,
      String primaryTopic);

  NewsWeeklySummaryResponse getWeeklySummary(String language, String portfolioSymbols);

  /** Haberin orijinal (kaynak) içeriğini döner. */
  NewsOriginalResponse getOriginalNews(Long id);

  /** Zenginleştirilmiş haber detayını dil tercihiyle döner. */
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
