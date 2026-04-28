package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.NewsEnrichedPageResponse;
import com.company.finance_api.service.NewsEnrichmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
public class NewsAggregationController {

    private final NewsEnrichmentService newsEnrichmentService;

    public NewsAggregationController(NewsEnrichmentService newsEnrichmentService) {
        this.newsEnrichmentService = newsEnrichmentService;
    }

    @GetMapping("/enriched")
    public ApiResponse<NewsEnrichedPageResponse> listEnriched(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(newsEnrichmentService.getEnrichedNews(page, size));
    }
}
