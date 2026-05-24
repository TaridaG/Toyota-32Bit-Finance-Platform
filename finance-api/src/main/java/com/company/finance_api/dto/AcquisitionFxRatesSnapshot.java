package com.company.finance_api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/**
 * FX hub snapshot (TRY crosses + USD legs) used for a trade preview or persisted on {@code
 * transaction_acquisition_fx}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AcquisitionFxRatesSnapshot(
    /** ISO-8601 end of acquisition window used for MDS "at or before" queries (UTC). */
    String fxAsOfIso,
    BigDecimal usdTry,
    BigDecimal eurTry,
    BigDecimal gbpTry,
    BigDecimal jpyTry,
    BigDecimal aedTry,
    BigDecimal eurUsd,
    BigDecimal gbpUsd,
    BigDecimal jpyUsd) {}
