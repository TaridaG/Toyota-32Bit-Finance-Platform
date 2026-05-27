package com.company.finance_api.portfolio.infrastructure.http;

import com.company.finance_api.domain.PortfolioSnapshot;
import com.company.finance_api.dto.PortfolioOverviewResponse;
import com.company.finance_api.dto.PortfolioPerformanceSeriesResponse;
import com.company.finance_api.dto.PortfolioPositionResponse;
import com.company.finance_api.dto.PortfolioSummaryResponse;
import com.company.finance_api.dto.PortfolioTradeFlowResponse;
import com.company.finance_api.dto.PortfolioValuationResponse;
import com.company.finance_api.portfolio.application.PortfolioOverviewService;
import com.company.finance_api.portfolio.application.PortfolioPerformanceSeriesService;
import com.company.finance_api.portfolio.application.PortfolioService;
import com.company.finance_api.portfolio.application.PortfolioSnapshotService;
import com.company.finance_api.portfolio.application.PortfolioTradeFlowService;
import com.company.finance_api.portfolio.application.PortfolioValuationService;
import com.company.finance_api.shared.web.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Portfolio özet, değerleme ve snapshot endpoint'lerini sunan controller. */
@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

  private final PortfolioService portfolioService;
  private final PortfolioOverviewService portfolioOverviewService;
  private final PortfolioPerformanceSeriesService portfolioPerformanceSeriesService;
  private final PortfolioSnapshotService portfolioSnapshotService;
  private final PortfolioValuationService portfolioValuationService;
  private final PortfolioTradeFlowService portfolioTradeFlowService;

  /** Kullanıcının tüm portfolio pozisyonlarını listeler. */
  @GetMapping
  public ApiResponse<List<PortfolioPositionResponse>> myPortfolio() {
    return ApiResponse.success(portfolioService.getMyPortfolio());
  }

  /** Portfolio özet metriklerini döner. */
  @GetMapping("/summary")
  public ApiResponse<PortfolioSummaryResponse> summary() {
    return ApiResponse.success(portfolioService.getPortfolioSummary());
  }

  /** Hedef para biriminde portfolio genel bakışını döner. */
  @GetMapping("/overview")
  public ApiResponse<PortfolioOverviewResponse> overview(
      @RequestHeader(value = "X-Currency", required = false) String targetCurrency,
      @RequestParam(value = "portfolioId", required = false) Long portfolioId) {
    return ApiResponse.success(portfolioOverviewService.getMyOverview(targetCurrency, portfolioId));
  }

  /** Portfolio değerleme detaylarını döner. */
  @GetMapping("/valuation")
  public ApiResponse<PortfolioValuationResponse> valuation() {
    return ApiResponse.success(portfolioValuationService.getMyValuation());
  }

  /** Belirtilen portfolio için geçmiş snapshot kayıtlarını listeler. */
  @GetMapping("/snapshots")
  public ApiResponse<List<PortfolioSnapshot>> snapshots(
      @RequestParam(value = "portfolioId") Long portfolioId) {
    return ApiResponse.success(portfolioSnapshotService.getMySnapshots(portfolioId));
  }

  /** Portfolio performans serisini hedef para biriminde döner. */
  @GetMapping("/performance-series")
  public ApiResponse<PortfolioPerformanceSeriesResponse> performanceSeries(
      @RequestHeader(value = "X-Currency", required = false) String targetCurrency,
      @RequestParam(value = "portfolioId", required = false) Long portfolioId,
      @RequestParam(value = "range", required = false) String range) {
    return ApiResponse.success(
        portfolioPerformanceSeriesService.getMyPerformanceSeries(targetCurrency, portfolioId, range));
  }

  /** Portfolio alım-satım akışı metriklerini döner. */
  @GetMapping("/trade-flow")
  public ApiResponse<PortfolioTradeFlowResponse> tradeFlow(
      @RequestHeader(value = "X-Currency", required = false) String targetCurrency,
      @RequestParam(value = "portfolioId", required = false) Long portfolioId) {
    return ApiResponse.success(
        portfolioTradeFlowService.getMyTradeFlow(targetCurrency, portfolioId));
  }
}
