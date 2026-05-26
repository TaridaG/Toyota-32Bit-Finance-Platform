package com.company.finance_api.portfolio.application;

import com.company.finance_api.portfolio.infrastructure.http.dto.TransactionHistoryPageResponse;
import com.company.finance_api.portfolio.infrastructure.http.dto.TransactionHistoryResponse;
import java.time.LocalDate;
import java.util.List;

/** TransactionHistoryService iş mantığını uygular (transaction history service). */
public interface TransactionHistoryService {
  /** getMyHistory sözleşmesi. */
  List<TransactionHistoryResponse> getMyHistory(Long portfolioId);

  TransactionHistoryPageResponse getMyHistoryPage(
      int page,
      int size,
      Long portfolioId,
      String symbol,
      String type,
      String purchaseMode,
      String inputCurrency,
      LocalDate fromDate,
      LocalDate toDate);
}
