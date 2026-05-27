package com.company.finance_api.portfolio.external.infrastructure.http;

import com.company.finance_api.portfolio.external.dto.CreateExternalPortfolioRequest;
import com.company.finance_api.portfolio.external.dto.CreateExternalPositionRequest;
import com.company.finance_api.portfolio.external.dto.ExternalPortfolioAllocationResponse;
import com.company.finance_api.portfolio.external.dto.ExternalPortfolioResponse;
import com.company.finance_api.portfolio.external.dto.ExternalPortfolioSummaryResponse;
import com.company.finance_api.portfolio.external.dto.PatchExternalPortfolioRequest;
import com.company.finance_api.portfolio.external.service.ExternalPortfolioService;
import com.company.finance_api.portfolio.external.service.ExternalPortfolioValuationService;
import com.company.finance_api.shared.security.CurrentUserResolver;
import com.company.finance_api.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** External portfolio ve pozisyon yönetimi için REST endpoint'leri sağlayan controller. */
@RestController
@RequestMapping("/api/v1/external/portfolios")
@RequiredArgsConstructor
public class ExternalPortfolioController {

  private final ExternalPortfolioService service;
  private final ExternalPortfolioValuationService valuationService;
  private final CurrentUserResolver currentUserResolver;

  /** Yeni external portfolio oluşturur. */
  @PostMapping
  public ApiResponse<ExternalPortfolioResponse> create(
      @Valid @RequestBody CreateExternalPortfolioRequest request) {
    UUID userId = currentUserResolver.getCurrentUserId();
    return ApiResponse.success(service.createPortfolio(userId, request));
  }

  /** Oturum açmış kullanıcının external portfolio listesini döner. */
  @GetMapping
  public ApiResponse<List<ExternalPortfolioResponse>> list() {
    UUID userId = currentUserResolver.getCurrentUserId();
    return ApiResponse.success(service.getUserPortfolios(userId));
  }

  /** Tek bir external portfolio kaydını getirir. */
  @GetMapping("/{portfolioId}")
  public ApiResponse<ExternalPortfolioResponse> getOne(@PathVariable Long portfolioId) {
    UUID userId = currentUserResolver.getCurrentUserId();
    return ApiResponse.success(service.getPortfolio(userId, portfolioId));
  }

  /** External portfolio ayarlarını günceller. */
  @PatchMapping("/{portfolioId}")
  public ApiResponse<ExternalPortfolioResponse> patch(
      @PathVariable Long portfolioId, @Valid @RequestBody PatchExternalPortfolioRequest request) {
    UUID userId = currentUserResolver.getCurrentUserId();
    return ApiResponse.success(service.patchPortfolio(userId, portfolioId, request));
  }

  /** External portfolio'yu siler. */
  @DeleteMapping("/{portfolioId}")
  public ApiResponse<Void> delete(@PathVariable Long portfolioId) {
    UUID userId = currentUserResolver.getCurrentUserId();
    service.deletePortfolio(userId, portfolioId);
    return ApiResponse.success(null);
  }

  /** Portfolio'ya yeni pozisyon ekler. */
  @PostMapping("/{portfolioId}/positions")
  public ApiResponse<Void> addPosition(
      @PathVariable Long portfolioId, @Valid @RequestBody CreateExternalPositionRequest request) {
    UUID userId = currentUserResolver.getCurrentUserId();
    service.addPosition(userId, portfolioId, request);
    return ApiResponse.success(null);
  }

  /** Portfolio'dan pozisyon siler. */
  @DeleteMapping("/{portfolioId}/positions/{positionId}")
  public ApiResponse<Void> deletePosition(
      @PathVariable Long portfolioId, @PathVariable Long positionId) {
    UUID userId = currentUserResolver.getCurrentUserId();
    service.deletePosition(userId, portfolioId, positionId);
    return ApiResponse.success(null);
  }

  /** External portfolio değerleme özetini döner. */
  @GetMapping("/{portfolioId}/summary")
  public ApiResponse<ExternalPortfolioSummaryResponse> summary(@PathVariable Long portfolioId) {
    UUID userId = currentUserResolver.getCurrentUserId();
    return ApiResponse.success(valuationService.calculateSummary(userId, portfolioId));
  }

  /** External portfolio varlık dağılımını (allocation) döner. */
  @GetMapping("/{portfolioId}/allocation")
  public ApiResponse<List<ExternalPortfolioAllocationResponse>> allocation(
      @PathVariable Long portfolioId) {
    UUID userId = currentUserResolver.getCurrentUserId();
    return ApiResponse.success(valuationService.calculateAllocation(userId, portfolioId));
  }
}
