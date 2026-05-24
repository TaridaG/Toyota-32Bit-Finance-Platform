package com.company.marketdataservice.history.application;
import com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto;
import com.company.marketdataservice.spot.infrastructure.http.dto.MarketPriceSummaryDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * `geçmiş veri ve backfill` application katmanı use-case servisi.
 */
public interface HistoricalMarketDataReadService {

    List<HistoryPointDto> getPriceHistory(String symbol, LocalDate from, LocalDate to);

    List<HistoryPointDto> getFxHistory(String symbol, LocalDate from, LocalDate to);

    List<HistoryPointDto> getFundHistory(String fundCode, LocalDate from, LocalDate to);

    Map<String, MarketPriceSummaryDto> getPriceSummary(List<String> symbols);
}
