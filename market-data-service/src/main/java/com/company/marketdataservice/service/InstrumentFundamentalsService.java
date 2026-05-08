package com.company.marketdataservice.service;

import com.company.marketdataservice.dto.InstrumentFundamentalsDto;

public interface InstrumentFundamentalsService {
    InstrumentFundamentalsDto getFundamentals(String symbol, boolean forceRefresh);
}
