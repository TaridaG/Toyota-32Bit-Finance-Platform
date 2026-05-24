package com.company.finance_api.dto.market.eurobond;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** EurobondInstrumentDto — API transfer nesnesi (DTO/response/request). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EurobondInstrumentDto(
    String displayName,
    String isin,
    String symbol,
    String name,
    String issuer,
    String currency,
    LocalDate maturityDate,
    BigDecimal remainingYears,
    BigDecimal couponPercent,
    String couponFrequency,
    BigDecimal cleanPrice,
    BigDecimal bidPrice,
    BigDecimal askPrice,
    BigDecimal yieldToMaturityPercent,
    BigDecimal dailyChangePercent,
    String sourceProvider,
    Instant lastUpdatedAt) {}
