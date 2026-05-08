package com.company.marketdataservice.fundamentals.provider;

import com.company.marketdataservice.dto.InstrumentFundamentalsDto;
import com.company.marketdataservice.instrument.InstrumentCatalogEntry;

public interface InstrumentFundamentalsProvider {
    String providerCode();

    boolean supports(InstrumentCatalogEntry instrument);

    InstrumentFundamentalsDto fetch(InstrumentCatalogEntry instrument, String providerSymbol);
}

