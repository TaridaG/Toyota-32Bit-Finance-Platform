package com.company.finance_api.portfolio.infrastructure.http;

import com.company.finance_api.portfolio.domain.Transaction;
import com.company.finance_api.pricing.infrastructure.http.dto.InstrumentPriceCoverageResponse;
import com.company.finance_api.portfolio.infrastructure.http.dto.TradeExecutionRequest;
import com.company.finance_api.portfolio.infrastructure.http.dto.TradeExecutionResponse;
import com.company.finance_api.portfolio.infrastructure.http.dto.TradePreviewResponse;
import com.company.finance_api.portfolio.application.TradeService;
import com.company.finance_api.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Alım/satım işlemleri ve önizleme için REST endpoint'leri sağlayan controller. */
@RestController
@RequestMapping("/api/v1/trades")
@RequiredArgsConstructor
public class TradeController {

  private final TradeService tradeService;

  /** Basit parametrelerle alım işlemi gerçekleştirir. */
  @PostMapping("/buy")
  public ApiResponse<Transaction> buy(
      @RequestParam Long instrumentId, @RequestParam BigDecimal quantity) {
    return ApiResponse.success(tradeService.buy(instrumentId, quantity));
  }

  /** Detaylı alım emri isteği ile işlem gerçekleştirir. */
  @PostMapping("/buy/order")
  public ApiResponse<TradeExecutionResponse> buyOrder(
      @Valid @RequestBody TradeExecutionRequest request) {
    Transaction tx = tradeService.buy(request);
    return ApiResponse.success(toResponse(tx));
  }

  /** Basit parametrelerle satış işlemi gerçekleştirir. */
  @PostMapping("/sell")
  public ApiResponse<Transaction> sell(
      @RequestParam Long instrumentId, @RequestParam BigDecimal quantity) {
    return ApiResponse.success(tradeService.sell(instrumentId, quantity));
  }

  /** Detaylı satış emri isteği ile işlem gerçekleştirir. */
  @PostMapping("/sell/order")
  public ApiResponse<TradeExecutionResponse> sellOrder(
      @Valid @RequestBody TradeExecutionRequest request) {
    Transaction tx = tradeService.sell(request);
    return ApiResponse.success(toResponse(tx));
  }

  /** İşlem önizlemesi hesaplar (gerçekleştirmeden). */
  @PostMapping("/preview")
  public ApiResponse<TradePreviewResponse> preview(
      @Valid @RequestBody TradeExecutionRequest request) {
    return ApiResponse.success(tradeService.preview(request));
  }

  /** Enstrüman için fiyat kapsama bilgisini döner. */
  @GetMapping("/instruments/{instrumentId}/price-coverage")
  public ApiResponse<InstrumentPriceCoverageResponse> priceCoverage(
      @PathVariable Long instrumentId) {
    return ApiResponse.success(tradeService.getPriceCoverage(instrumentId));
  }

  private TradeExecutionResponse toResponse(Transaction tx) {
    return new TradeExecutionResponse(
        tx.getId(),
        tx.getInstrument().getId(),
        tx.getInstrument().getSymbol(),
        tx.getType().name(),
        tx.getPurchaseMode().name(),
        tx.getQuantity(),
        tx.getPrice(),
        tx.getTotalAmount(),
        tx.getInputCurrency(),
        tx.getInputAmount(),
        tx.getFxRateUsed(),
        tx.getAcquiredAt(),
        tx.getCreatedAt());
  }
}
