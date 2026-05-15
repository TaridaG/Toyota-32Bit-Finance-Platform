package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.market.eurobond.EurobondCashflowResponse;
import com.company.finance_api.dto.market.eurobond.EurobondHistoryResponse;
import com.company.finance_api.dto.market.eurobond.EurobondInstrumentDto;
import com.company.finance_api.service.TrEurobondMarketService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/eurobonds/tr")
public class MarketTurkeyEurobondController {

    private final TrEurobondMarketService trEurobondMarketService;

    public MarketTurkeyEurobondController(TrEurobondMarketService trEurobondMarketService) {
        this.trEurobondMarketService = trEurobondMarketService;
    }

    @GetMapping("/instruments")
    public ApiResponse<List<EurobondInstrumentDto>> instruments() {
        return ApiResponse.success(trEurobondMarketService.listActiveInstruments());
    }

    @GetMapping("/instruments/{isin}")
    public ApiResponse<EurobondInstrumentDto> instrument(@PathVariable("isin") String isin) {
        return ApiResponse.success(trEurobondMarketService.getInstrument(isin));
    }

    @GetMapping("/instruments/{isin}/history")
    public ApiResponse<EurobondHistoryResponse> history(
            @PathVariable("isin") String isin,
            @RequestParam(name = "range", required = false, defaultValue = "5Y") String range,
            @RequestParam(name = "frequency", required = false, defaultValue = "DAILY") String frequency
    ) {
        return ApiResponse.success(trEurobondMarketService.getHistory(isin, range, frequency));
    }

    @GetMapping("/cashflow")
    public ApiResponse<EurobondCashflowResponse> cashflow(
            @RequestParam("isin") String isin,
            @RequestParam("nominal") BigDecimal nominal
    ) {
        return ApiResponse.success(trEurobondMarketService.cashflow(isin, nominal));
    }
}
