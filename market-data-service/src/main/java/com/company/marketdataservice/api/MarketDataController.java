package com.company.marketdataservice.api;

import com.company.marketdataservice.dto.FundDto;
import com.company.marketdataservice.dto.FxRateDto;
import com.company.marketdataservice.dto.MarketPriceDto;
import com.company.marketdataservice.service.MarketDataReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataReadService marketDataReadService;

    @GetMapping("/prices")
    public List<MarketPriceDto> prices() {
        return marketDataReadService.getLatestPrices();
    }

    @GetMapping("/fx")
    public List<FxRateDto> fx() {
        return marketDataReadService.getFxRates();
    }

    @GetMapping("/funds")
    public List<FundDto> funds() {
        return marketDataReadService.getFunds();
    }
}
