package com.company.marketdataservice.api;

import com.company.marketdataservice.dto.FundDto;
import com.company.marketdataservice.dto.FxRateDto;
import com.company.marketdataservice.dto.MarketPriceDto;
import com.company.marketdataservice.dto.MarketSegmentPulseResponse;
import com.company.marketdataservice.service.MarketDataReadService;
import com.company.marketdataservice.service.MarketSegmentPulseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataReadService marketDataReadService;
    private final MarketSegmentPulseService marketSegmentPulseService;

    @GetMapping("/prices")
    public List<MarketPriceDto> prices(@RequestParam(name = "segment", required = false) String segment) {
        return marketDataReadService.getLatestPrices(segment);
    }

    @GetMapping("/segments/pulse")
    public MarketSegmentPulseResponse segmentPulse() {
        return marketSegmentPulseService.getPulse();
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
