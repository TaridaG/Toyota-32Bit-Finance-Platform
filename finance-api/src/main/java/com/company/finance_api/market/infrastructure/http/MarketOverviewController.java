package com.company.finance_api.market.infrastructure.http;

import com.company.finance_api.dto.MarketInsightsResponse;
import com.company.finance_api.dto.MarketOverviewPageResponse;
import com.company.finance_api.service.MarketOverviewService;
import com.company.finance_api.shared.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Piyasa özeti ve insight REST endpoint'lerini sunar. */
@RestController
@RequestMapping("/api/market")
public class MarketOverviewController {

  private final MarketOverviewService marketOverviewService;

  public MarketOverviewController(MarketOverviewService marketOverviewService) {
    this.marketOverviewService = marketOverviewService;
  }

  /** GET /overview — kategori, arama ve sıralama ile sayfalanmış piyasa özeti döner. */
  @GetMapping("/overview")
  public ApiResponse<MarketOverviewPageResponse> overview(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String category,
      @RequestParam(required = false, name = "q") String search,
      @RequestParam(required = false) String sort,
      @RequestHeader(value = "X-Currency", required = false) String currency) {
    return ApiResponse.success(
        marketOverviewService.getOverview(page, size, category, search, currency, sort));
  }

  /** GET /insights — piyasa insight özetini {@code X-Currency} başlığına göre döner. */
  @GetMapping("/insights")
  public ApiResponse<MarketInsightsResponse> insights(
      @RequestHeader(value = "X-Currency", required = false) String currency) {
    return ApiResponse.success(marketOverviewService.getInsights(currency));
  }
}
