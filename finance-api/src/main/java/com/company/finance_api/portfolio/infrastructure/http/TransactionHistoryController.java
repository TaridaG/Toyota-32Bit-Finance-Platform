package com.company.finance_api.portfolio.infrastructure.http;

import com.company.finance_api.dto.TransactionHistoryPageResponse;
import com.company.finance_api.dto.TransactionHistoryResponse;
import com.company.finance_api.service.TransactionHistoryService;
import com.company.finance_api.shared.web.ApiResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/** Transaction geçmişi sorguları için REST endpoint'leri sağlayan controller. */
@RestController
@RequestMapping("/api/history/transactions")
@RequiredArgsConstructor
public class TransactionHistoryController {

  private final TransactionHistoryService historyService;

  /** Kullanıcının transaction geçmişini listeler. */
  @GetMapping
  public ApiResponse<List<TransactionHistoryResponse>> myTransactions(
      @RequestParam(required = false) Long portfolioId) {
    return ApiResponse.success(historyService.getMyHistory(portfolioId));
  }

  /** Filtre ve sayfalama ile transaction geçmişi sayfasını döner. */
  @GetMapping("/page")
  public ApiResponse<TransactionHistoryPageResponse> myTransactionsPage(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) Long portfolioId,
      @RequestParam(required = false) String symbol,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String purchaseMode,
      @RequestParam(required = false) String inputCurrency,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate toDate) {
    return ApiResponse.success(
        historyService.getMyHistoryPage(
            page, size, portfolioId, symbol, type, purchaseMode, inputCurrency, fromDate, toDate));
  }
}
