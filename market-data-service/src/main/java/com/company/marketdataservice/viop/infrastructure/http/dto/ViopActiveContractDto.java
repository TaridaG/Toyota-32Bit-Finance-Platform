package com.company.marketdataservice.viop.infrastructure.http.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Aktif VIOP sözleşmesi ve son settlement özeti için API transfer nesnesi (DTO/response).
 */
public record ViopActiveContractDto(
        String contractCode,
        String underlying,
        String marketGroup,
        LocalDate expiryDate,
        LocalDate tradeDate,
        BigDecimal settlementPrice,
        BigDecimal changePercent,
        BigDecimal volumeTl,
        BigDecimal volumeQty,
        BigDecimal openInterest,
        Instant ingestedAt
) {}

