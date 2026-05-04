package com.company.marketdataservice.service;

import com.company.marketdataservice.dto.HistoryPointDto;
import com.company.marketdataservice.dto.MarketPriceSummaryDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface HistoricalMarketDataReadService {

    List<HistoryPointDto> getPriceHistory(String symbol, LocalDate from, LocalDate to);

    List<HistoryPointDto> getFxHistory(String symbol, LocalDate from, LocalDate to);

    List<HistoryPointDto> getFundHistory(String fundCode, LocalDate from, LocalDate to);

    Map<String, MarketPriceSummaryDto> getPriceSummary(List<String> symbols);
}
