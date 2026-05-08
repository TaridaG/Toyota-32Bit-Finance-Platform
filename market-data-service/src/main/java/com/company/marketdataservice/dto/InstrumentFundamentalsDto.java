package com.company.marketdataservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record InstrumentFundamentalsDto(
        String symbol,
        String provider,
        String providerSymbol,
        String companyName,
        String country,
        String currency,
        String exchange,
        String ipoDate,
        String industry,
        String website,
        BigDecimal marketCapitalization,
        BigDecimal sharesOutstanding,
        BigDecimal peTtm,
        BigDecimal epsTtm,
        Instant fetchedAt,
        boolean cacheHit,
        List<AnnualFinancialStatementDto> annualStatements
) {
    public InstrumentFundamentalsDto withCacheHit(boolean cacheHit) {
        return new InstrumentFundamentalsDto(
                symbol,
                provider,
                providerSymbol,
                companyName,
                country,
                currency,
                exchange,
                ipoDate,
                industry,
                website,
                marketCapitalization,
                sharesOutstanding,
                peTtm,
                epsTtm,
                fetchedAt,
                cacheHit,
                annualStatements
        );
    }
}
