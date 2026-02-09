package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.TransactionHistoryResponse;
import com.company.finance_api.service.TransactionHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history/transactions")
@RequiredArgsConstructor
public class TransactionHistoryController {

    private final TransactionHistoryService historyService;

    @GetMapping
    public ApiResponse<List<TransactionHistoryResponse>> myTransactions() {
        return ApiResponse.success(historyService.getMyHistory());
    }
}