package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.NewsEnrichedDetailResponse;
import com.company.finance_api.dto.NewsEnrichedPageResponse;
import com.company.finance_api.dto.NewsOriginalResponse;
import com.company.finance_api.service.NewsEnrichmentService;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsAggregationController {

    private final NewsEnrichmentService newsEnrichmentService;

    public NewsAggregationController(NewsEnrichmentService newsEnrichmentService) {
        this.newsEnrichmentService = newsEnrichmentService;
    }

    @GetMapping("/enriched")
    public ApiResponse<NewsEnrichedPageResponse> listEnriched(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "sentiment", required = false) String sentiment,
            @RequestParam(name = "maxAgeMinutes", required = false) Integer maxAgeMinutes,
            @RequestParam(name = "q", required = false) String search,
            @RequestHeader(value = "X-Language", required = false) String language
    ) {
        return ApiResponse.success(
                newsEnrichmentService.getEnrichedNews(page, size, language, category, sentiment, maxAgeMinutes, search)
        );
    }

    @GetMapping("/enriched/chart")
    public ApiResponse<List<com.company.finance_api.dto.NewsEnrichedResponse>> chartEnriched(
            @RequestParam String symbol,
            @RequestParam String category,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestHeader(value = "X-Language", required = false) String language
    ) {
        return ApiResponse.success(
                newsEnrichmentService.getEnrichedChartNews(symbol, category, from, to, language)
        );
    }

    @GetMapping("/enriched/{id}")
    public ApiResponse<NewsEnrichedDetailResponse> getEnrichedDetail(
            @PathVariable Long id,
            @RequestHeader(value = "X-Language", required = false) String language
    ) {
        return ApiResponse.success(newsEnrichmentService.getEnrichedNewsDetail(id, language));
    }

    @GetMapping("/enriched/{id}/original")
    public ApiResponse<NewsOriginalResponse> getOriginal(@PathVariable Long id) {
        return ApiResponse.success(newsEnrichmentService.getOriginalNews(id));
    }
}
