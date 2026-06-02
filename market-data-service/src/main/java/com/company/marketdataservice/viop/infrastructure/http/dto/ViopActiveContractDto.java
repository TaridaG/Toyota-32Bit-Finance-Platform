package com.company.marketdataservice.viop.infrastructure.http.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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

