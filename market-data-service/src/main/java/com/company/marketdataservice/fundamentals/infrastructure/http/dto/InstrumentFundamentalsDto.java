package com.company.marketdataservice.fundamentals.infrastructure.http.dto;
import com.company.marketdataservice.fundamentals.infrastructure.http.dto.AnnualFinancialStatementDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * `temel veri (fundamentals)` REST API için HTTP DTO.
 */
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
