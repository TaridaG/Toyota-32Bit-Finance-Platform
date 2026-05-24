package com.company.marketdataservice.fundamentals.infrastructure.provider;
import com.company.marketdataservice.fundamentals.infrastructure.http.dto.InstrumentFundamentalsDto;
import com.company.marketdataservice.catalog.infrastructure.persistence.InstrumentCatalogEntry;

/**
 * `temel veri (fundamentals)` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
public interface InstrumentFundamentalsProvider {
    String providerCode();

    boolean supports(InstrumentCatalogEntry instrument);

    InstrumentFundamentalsDto fetch(InstrumentCatalogEntry instrument, String providerSymbol);
}

