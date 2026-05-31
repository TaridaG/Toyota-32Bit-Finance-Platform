package com.company.finance_api.portfolio.infrastructure.http.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** TradePreviewResponse — API transfer nesnesi (DTO/response/request). */
public record TradePreviewResponse(
    Long instrumentId,
    String instrumentSymbol,
    String instrumentQuoteCurrency,
    BigDecimal computedLots,
    BigDecimal computedInputAmount,
    String inputCurrency,
    BigDecimal unitPriceUsed,
    BigDecimal fxRateUsed,
    boolean manualUnitPriceRequired,
    String unitPriceSource,
    /**
     * For {@code PAST}: acquisition instant used after any rollback to earliest available price
     * day.
     */
    Instant effectiveAcquiredAt,
    /**
     * True when the requested past date had no price and the system used the earliest available day
     * instead.
     */
    boolean pastDateRolledToEarliestData,
    /**
     * TRY-hub (+ USD legs) FX panel at conversion time (MDS for {@code PAST}, live for {@code
     * NOW}).
     */
    AcquisitionFxRatesSnapshot acquisitionFxRates) {}
