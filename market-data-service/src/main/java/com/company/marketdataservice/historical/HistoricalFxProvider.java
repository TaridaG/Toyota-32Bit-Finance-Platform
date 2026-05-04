package com.company.marketdataservice.historical;

import java.time.LocalDate;
import java.util.List;

public interface HistoricalFxProvider {

    List<HistoricalFxPoint> fetchRange(String symbol, LocalDate startDate, LocalDate endDate);
}
