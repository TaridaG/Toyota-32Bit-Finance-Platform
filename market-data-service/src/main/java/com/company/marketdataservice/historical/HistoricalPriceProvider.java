package com.company.marketdataservice.historical;

import java.time.LocalDate;
import java.util.List;

public interface HistoricalPriceProvider {

    List<HistoricalPricePoint> fetchRange(String symbol, LocalDate startDate, LocalDate endDate);
}
