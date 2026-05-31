package com.company.finance_api.news.infrastructure.http;

import com.company.finance_api.news.application.NewsEnrichmentService;
import com.company.finance_api.news.infrastructure.http.dto.NewsEnrichedDetailResponse;
import com.company.finance_api.news.infrastructure.http.dto.NewsEnrichedPageResponse;
import com.company.finance_api.news.infrastructure.http.dto.NewsEnrichedResponse;
import com.company.finance_api.news.infrastructure.http.dto.NewsOriginalResponse;
import com.company.finance_api.news.infrastructure.http.dto.NewsWeeklySummaryResponse;
import com.company.finance_api.shared.web.ApiResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Haber toplama ve zenginleştirilmiş haber detay endpoint'lerini sunar. */
@RestController
@RequestMapping("/api/v1/news")
public class NewsAggregationController {

  private final NewsEnrichmentService newsEnrichmentService;

  public NewsAggregationController(NewsEnrichmentService newsEnrichmentService) {
    this.newsEnrichmentService = newsEnrichmentService;
  }

  /** HTTP handler — Liste endpoint'i — kayıtları döner. */
  @GetMapping("/enriched")
  public ApiResponse<NewsEnrichedPageResponse> listEnriched(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "category", required = false) String category,
      @RequestParam(name = "sentiment", required = false) String sentiment,
      @RequestParam(name = "maxAgeMinutes", required = false) Integer maxAgeMinutes,
      @RequestParam(name = "q", required = false) String search,
      @RequestParam(name = "relatedSymbols", required = false) String relatedSymbols,
      @RequestParam(name = "sourceName", required = false) String sourceName,
      @RequestParam(name = "assetKey", required = false) String assetKey,
      @RequestParam(name = "primaryTopic", required = false) String primaryTopic,
      @RequestHeader(value = "X-Language", required = false) String language) {
    return ApiResponse.success(
        newsEnrichmentService.getEnrichedNews(
            page,
            size,
            language,
            category,
            sentiment,
            maxAgeMinutes,
            search,
            relatedSymbols,
            sourceName,
            assetKey,
            primaryTopic));
  }

  @GetMapping("/enriched/weekly-summary")
  public ApiResponse<NewsWeeklySummaryResponse> weeklySummary(
      @RequestParam(name = "portfolioSymbols", required = false) String portfolioSymbols,
      @RequestHeader(value = "X-Language", required = false) String language) {
    return ApiResponse.success(newsEnrichmentService.getWeeklySummary(language, portfolioSymbols));
  }

  @GetMapping("/enriched/chart")
  public ApiResponse<List<NewsEnrichedResponse>> chartEnriched(
      @RequestParam String symbol,
      @RequestParam String category,
      @RequestParam Instant from,
      @RequestParam Instant to,
      @RequestHeader(value = "X-Language", required = false) String language) {
    return ApiResponse.success(
        newsEnrichmentService.getEnrichedChartNews(symbol, category, from, to, language));
  }

  /** HTTP handler — Tekil kayıt veya koleksiyon döner. */
  @GetMapping("/enriched/{id:\\d+}")
  public ApiResponse<NewsEnrichedDetailResponse> getEnrichedDetail(
      @PathVariable Long id,
      @RequestHeader(value = "X-Language", required = false) String language) {
    return ApiResponse.success(newsEnrichmentService.getEnrichedNewsDetail(id, language));
  }

  /** HTTP handler — Tekil kayıt veya koleksiyon döner. */
  @GetMapping("/enriched/{id:\\d+}/original")
  public ApiResponse<NewsOriginalResponse> getOriginal(@PathVariable Long id) {
    return ApiResponse.success(newsEnrichmentService.getOriginalNews(id));
  }
}
