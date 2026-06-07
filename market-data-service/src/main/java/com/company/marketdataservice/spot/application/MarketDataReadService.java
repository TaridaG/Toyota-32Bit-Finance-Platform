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
     * Birleştirilmiş son fiyatları döner; isteğe bağlı olarak UI segment id'sine ({@code crypto}, {@code bist} vb.)
     * göre filtreler — segment tanımı {@link MarketCatalogSegmentRules#pulseSegment} ile uyumludur.
     */
    List<MarketPriceDto> getLatestPrices(String segment);

    List<FxRateDto> getFxRates();

    List<FundDto> getFunds();
}
