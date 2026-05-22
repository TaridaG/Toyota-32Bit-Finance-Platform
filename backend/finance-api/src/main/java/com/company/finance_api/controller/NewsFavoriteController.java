package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.AddNewsFavoriteRequest;
import com.company.finance_api.dto.NewsEnrichedPageResponse;
import com.company.finance_api.dto.NewsFavoriteItemDto;
import com.company.finance_api.service.NewsEnrichmentService;
import com.company.finance_api.service.NewsFavoriteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news/favorites")
public class NewsFavoriteController {

    private final NewsFavoriteService newsFavoriteService;
    private final NewsEnrichmentService newsEnrichmentService;

    public NewsFavoriteController(NewsFavoriteService newsFavoriteService, NewsEnrichmentService newsEnrichmentService) {
        this.newsFavoriteService = newsFavoriteService;
        this.newsEnrichmentService = newsEnrichmentService;
    }

    @GetMapping("/enriched")
    public ApiResponse<NewsEnrichedPageResponse> listEnrichedFavorites(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "maxAgeMinutes", required = false) Integer maxAgeMinutes,
            @RequestParam(name = "q", required = false) String search,
            @RequestHeader(value = "X-Language", required = false) String language
    ) {
        return ApiResponse.success(
                newsEnrichmentService.getEnrichedFavoriteNews(page, size, language, category, maxAgeMinutes, search)
        );
    }

    @GetMapping
    public ApiResponse<List<NewsFavoriteItemDto>> getMyFavorites() {
        return ApiResponse.success(newsFavoriteService.getMyFavorites());
    }

    @PostMapping
    public ApiResponse<Void> addFavorite(@Valid @RequestBody AddNewsFavoriteRequest request) {
        newsFavoriteService.addFavorite(request.getNewsId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{newsId}")
    public ApiResponse<Void> removeFavorite(@PathVariable Long newsId) {
        newsFavoriteService.removeFavorite(newsId);
        return ApiResponse.success(null);
    }
}
