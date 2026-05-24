package com.company.finance_api.news.infrastructure.http;

import com.company.finance_api.dto.AddNewsFavoriteRequest;
import com.company.finance_api.dto.NewsEnrichedPageResponse;
import com.company.finance_api.dto.NewsFavoriteItemDto;
import com.company.finance_api.service.NewsEnrichmentService;
import com.company.finance_api.service.NewsFavoriteService;
import com.company.finance_api.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Kullanıcı haber favorileri endpoint'lerini sunar. */
@RestController
@RequestMapping("/api/news/favorites")
public class NewsFavoriteController {

  private final NewsFavoriteService newsFavoriteService;
  private final NewsEnrichmentService newsEnrichmentService;

  public NewsFavoriteController(
      NewsFavoriteService newsFavoriteService, NewsEnrichmentService newsEnrichmentService) {
    this.newsFavoriteService = newsFavoriteService;
    this.newsEnrichmentService = newsEnrichmentService;
  }

  /** HTTP handler — Liste endpoint'i — kayıtları döner. */
  @GetMapping("/enriched")
  public ApiResponse<NewsEnrichedPageResponse> listEnrichedFavorites(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "category", required = false) String category,
      @RequestParam(name = "maxAgeMinutes", required = false) Integer maxAgeMinutes,
      @RequestParam(name = "q", required = false) String search,
      @RequestHeader(value = "X-Language", required = false) String language) {
    return ApiResponse.success(
        newsEnrichmentService.getEnrichedFavoriteNews(
            page, size, language, category, maxAgeMinutes, search));
  }

  /** HTTP handler — Tekil kayıt veya koleksiyon döner. */
  @GetMapping
  public ApiResponse<List<NewsFavoriteItemDto>> getMyFavorites() {
    return ApiResponse.success(newsFavoriteService.getMyFavorites());
  }

  @PostMapping
  public ApiResponse<Void> addFavorite(@Valid @RequestBody AddNewsFavoriteRequest request) {
    newsFavoriteService.addFavorite(request.getNewsId());
    return ApiResponse.success(null);
  }

  /** HTTP handler — İlişkili kaydı kaldırır. */
  @DeleteMapping("/{newsId}")
  public ApiResponse<Void> removeFavorite(@PathVariable Long newsId) {
    newsFavoriteService.removeFavorite(newsId);
    return ApiResponse.success(null);
  }
}
