package com.company.marketdataservice.spot.application;
import com.company.marketdataservice.catalog.domain.MarketCatalogSegmentRules;
import com.company.marketdataservice.spot.infrastructure.http.dto.FundDto;
import com.company.marketdataservice.spot.infrastructure.http.dto.FxRateDto;
import com.company.marketdataservice.spot.infrastructure.http.dto.MarketPriceDto;

import java.util.List;

/**
 * `spot fiyat` application katmanı use-case servisi.
 */
public interface MarketDataReadService {

    List<MarketPriceDto> getLatestPrices();

    /**
     * Latest merged prices, optionally restricted to a UI segment id (e.g. {@code crypto}, {@code bist})
     * as defined by {@link com.company.marketdataservice.catalog.domain.MarketCatalogSegmentRules#pulseSegment}.
     */
    List<MarketPriceDto> getLatestPrices(String segment);

    List<FxRateDto> getFxRates();

    List<FundDto> getFunds();
}
