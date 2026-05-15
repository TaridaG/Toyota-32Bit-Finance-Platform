package com.company.finance_api.service;

import com.company.finance_api.dto.AcquisitionFxRatesSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface CurrencyConversionService {

    BigDecimal convert(BigDecimal price, String from, String to);

    /**
     * Converts using FX mids from {@code mds_fx_rate_history} at or before {@code fxAsOfEndExclusive} (UTC).
     */
    BigDecimal convertAt(Instant fxAsOfEndExclusive, BigDecimal price, String from, String to);

    String normalizeCurrency(String currency);

    Optional<BigDecimal> getRate(String symbol);

    /**
     * TRY-hub (+ USD legs) panel for trade audit / preview.
     *
     * @param fxAsOfEndExclusive upper bound for MDS {@code observed_at} when {@code historical} is true;
     *                           when false, live {@link #convert} snapshot is used (instant is still echoed in the DTO).
     */
    AcquisitionFxRatesSnapshot acquisitionFxHubSnapshot(Instant fxAsOfEndExclusive, boolean historical);
}
