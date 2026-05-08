package com.company.finance_api.service;

import com.company.finance_api.dto.TransactionHistoryResponse;
import com.company.finance_api.dto.TransactionHistoryPageResponse;

import java.util.List;
import java.time.LocalDate;

public interface TransactionHistoryService {
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
            LocalDate toDate
    );
}