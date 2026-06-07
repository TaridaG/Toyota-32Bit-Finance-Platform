package com.company.marketdataservice.spot.infrastructure.http;
import com.company.marketdataservice.spot.infrastructure.http.dto.FundDto;
import com.company.marketdataservice.spot.infrastructure.http.dto.FxRateDto;
import com.company.marketdataservice.spot.infrastructure.http.dto.MarketPriceDto;
import com.company.marketdataservice.spot.infrastructure.http.dto.MarketSegmentPulseResponse;
import com.company.marketdataservice.spot.application.MarketDataReadService;
import com.company.marketdataservice.spot.application.MarketSegmentPulseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * `spot fiyat` REST endpoint'lerini expose eden HTTP controller.
 */
@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataReadService marketDataReadService;
    private final MarketSegmentPulseService marketSegmentPulseService;

    @GetMapping("/prices")
    public List<MarketPriceDto> prices(@RequestParam(name = "segment", required = false) String segment) {
        return marketDataReadService.getLatestPrices(segment);
    }

    /**
     * Segment bazlı piyasa nabzını (pulse) döndüren REST endpoint.
         */
    @GetMapping("/segments/pulse")
    public MarketSegmentPulseResponse segmentPulse() {
        return marketSegmentPulseService.getPulse();
    }

    /**
     * Güncel FX kurlarını listeleyen REST endpoint.
         */
    @GetMapping("/fx")
    public List<FxRateDto> fx() {
        return marketDataReadService.getFxRates();
    }

    /**
     * Takip edilen fon fiyatlarını listeleyen REST endpoint.
         */
    @GetMapping("/funds")
    public List<FundDto> funds() {
        return marketDataReadService.getFunds();
    }
}
