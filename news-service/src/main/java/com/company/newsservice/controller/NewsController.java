package com.company.newsservice.controller;

import com.company.newsservice.common.ApiResponse;
import com.company.newsservice.domain.enums.NewsCategory;
import com.company.newsservice.dto.NewsDetailResponse;
import com.company.newsservice.dto.NewsResponse;
import com.company.newsservice.service.NewsIngestionService;
import com.company.newsservice.service.NewsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsQueryService newsQueryService;
    private final NewsIngestionService newsIngestionService;

    @GetMapping
    public ApiResponse<Page<NewsResponse>> list(
            @RequestParam(required = false) NewsCategory category,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String lang,
            @RequestParam(defaultValue = "false") boolean includeOriginal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<NewsResponse> result = newsQueryService.search(category, q, PageRequest.of(page, size), lang, includeOriginal);
        return ApiResponse.success(result);
    }

    @GetMapping("/chart")
    public ApiResponse<List<NewsResponse>> chart(
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) String lang
    ) {
        return ApiResponse.success(newsQueryService.listForChart(from, to, lang));
    }

    @GetMapping("/{id}")
    public ApiResponse<NewsDetailResponse> detail(
            @PathVariable Long id,
            @RequestParam(required = false) String lang,
            @RequestParam(defaultValue = "true") boolean includeOriginal
    ) {
        return ApiResponse.success(newsQueryService.getById(id, lang, includeOriginal));
    }

    /**
     * Şimdilik admin koruması gateway ile yapılabilir.
     * İleride role-based admin endpoint’e taşırız.
     */
    @PostMapping("/admin/ingest")
    public ApiResponse<Integer> ingestNow() {
        return ApiResponse.success(newsIngestionService.ingestLatest());
    }
}