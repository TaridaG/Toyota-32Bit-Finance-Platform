package com.company.newsservice.query.infrastructure.http;

import com.company.newsservice.shared.web.ApiResponse;
import com.company.newsservice.query.domain.enums.NewsCategory;
import com.company.newsservice.query.infrastructure.http.dto.NewsDetailResponse;
import com.company.newsservice.query.infrastructure.http.dto.NewsResponse;
import com.company.newsservice.ingestion.application.IngestLatestNewsUseCase;
import com.company.newsservice.query.application.NewsQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Portal ve chart için haber REST endpoint'leri.
 */
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsQueryUseCase newsQueryUseCase;
    private final IngestLatestNewsUseCase ingestLatestNewsUseCase;

    /** Kategori ve metin filtresi ile sayfalanmış haber listesi döner. */
    @GetMapping
    public ApiResponse<Page<NewsResponse>> list(
            @RequestParam(required = false) NewsCategory category,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String lang,
            @RequestParam(defaultValue = "false") boolean includeOriginal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<NewsResponse> result = newsQueryUseCase.search(category, q, PageRequest.of(page, size), lang, includeOriginal);
        return ApiResponse.success(result);
    }

    /** Chart overlay için zaman aralığındaki haberleri döner. */
    @GetMapping("/chart")
    public ApiResponse<List<NewsResponse>> chart(
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) String lang
    ) {
        return ApiResponse.success(newsQueryUseCase.listForChart(from, to, lang));
    }

    /** Tek haber detayını id ile döner. */
    @GetMapping("/{id}")
    public ApiResponse<NewsDetailResponse> detail(
            @PathVariable Long id,
            @RequestParam(required = false) String lang,
            @RequestParam(defaultValue = "true") boolean includeOriginal
    ) {
        return ApiResponse.success(newsQueryUseCase.getById(id, lang, includeOriginal));
    }

    /** Manuel ingest tetikler; {@link com.company.newsservice.bootstrap.config.SecurityConfig} ile {@code ROLE_ADMIN} gerektirir. */
    @PostMapping("/admin/ingest")
    public ApiResponse<Integer> ingestNow() {
        return ApiResponse.success(ingestLatestNewsUseCase.ingestLatest());
    }
}