package com.company.finance_api.portfolio.infrastructure.http;

import com.company.finance_api.portfolio.application.SalesAnalysisService;
import com.company.finance_api.portfolio.infrastructure.http.dto.SalesAnalysisPageResponse;
import com.company.finance_api.shared.web.ApiResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Sayfalı realized sell analytics endpoint'lerini sunan controller. */
@RestController
@RequestMapping("/api/v1/history/sales-analysis")
@RequiredArgsConstructor
public class SalesAnalysisController {

  private final SalesAnalysisService salesAnalysisService;

  /** Kullanıcının kapanmış satışlarını filtre ve sayfalama ile listeler (realized P/L analytics). */
  @GetMapping("/page")
  public ApiResponse<SalesAnalysisPageResponse> mySalesAnalysisPage(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) Long portfolioId,
      @RequestParam(required = false) String symbol,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate toDate) {
    return ApiResponse.success(
        salesAnalysisService.getMySalesAnalysisPage(
            page, size, portfolioId, symbol, fromDate, toDate));
  }
}
