package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.domain.Transaction;
import com.company.finance_api.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @PostMapping("/buy")
    public ApiResponse<Transaction> buy(
            @RequestParam Long instrumentId,
            @RequestParam BigDecimal quantity
    ) {
        return ApiResponse.success(
                tradeService.buy(instrumentId, quantity)
        );
    }

    @PostMapping("/sell")
    public ApiResponse<Transaction> sell(
            @RequestParam Long instrumentId,
            @RequestParam BigDecimal quantity
    ) {
        return ApiResponse.success(
                tradeService.sell(instrumentId, quantity)
        );
    }
}