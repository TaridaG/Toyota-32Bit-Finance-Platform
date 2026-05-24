package com.company.marketdataservice.fundamentals.application;
import com.company.marketdataservice.fundamentals.infrastructure.http.dto.InstrumentFundamentalsDto;

/**
 * `temel veri (fundamentals)` application katmanı use-case servisi.
 */
public interface InstrumentFundamentalsService {
    InstrumentFundamentalsDto getFundamentals(String symbol, boolean forceRefresh);
}
