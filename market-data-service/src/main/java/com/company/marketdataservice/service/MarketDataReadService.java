package com.company.marketdataservice.service;

import com.company.marketdataservice.dto.FundDto;
import com.company.marketdataservice.dto.FxRateDto;
import com.company.marketdataservice.dto.MarketPriceDto;

import java.util.List;

public interface MarketDataReadService {

    List<MarketPriceDto> getLatestPrices();

    /**
     * Latest merged prices, optionally restricted to a UI segment id (e.g. {@code crypto}, {@code bist})
     * as defined by {@link com.company.marketdataservice.catalog.MarketCatalogSegmentRules#pulseSegment}.
     */
    List<MarketPriceDto> getLatestPrices(String segment);

    List<FxRateDto> getFxRates();

    List<FundDto> getFunds();
}
