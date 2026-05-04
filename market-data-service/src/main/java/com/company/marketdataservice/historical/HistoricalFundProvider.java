package com.company.marketdataservice.historical;

import java.time.LocalDate;
import java.util.List;

public interface HistoricalFundProvider {

    List<HistoricalFundPoint> fetchRange(String symbol, LocalDate startDate, LocalDate endDate);
}
