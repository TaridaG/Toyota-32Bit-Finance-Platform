package com.company.finance_api.service;

import com.company.finance_api.dto.TransactionHistoryResponse;
import java.util.List;

public interface TransactionHistoryService {
    List<TransactionHistoryResponse> getMyHistory();
}