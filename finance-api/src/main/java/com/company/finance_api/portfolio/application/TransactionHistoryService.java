package com.company.finance_api.portfolio.application;

import com.company.finance_api.portfolio.infrastructure.http.dto.TransactionHistoryPageResponse;
import com.company.finance_api.portfolio.infrastructure.http.dto.TransactionHistoryResponse;
import java.time.LocalDate;
import java.util.List;

/** TransactionHistoryService iş mantığını uygular (transaction history service). */
public interface TransactionHistoryService {
  /** Oturum açmış kullanıcının transaction geçmişini portfolio filtresiyle listeler. */
  List<TransactionHistoryResponse> getMyHistory(Long portfolioId);

  /** Transaction geçmişini sayfalama ve çoklu filtre ile döner. */
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

  /** Transaction'ı ledger'dan kaldırır; hiç gerçekleşmemiş gibi davranır (soft delete). */
  void deleteMyTransaction(Long transactionId);
}
