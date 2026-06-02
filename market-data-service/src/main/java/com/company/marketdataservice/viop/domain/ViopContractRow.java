package com.company.marketdataservice.viop.domain;

import java.time.LocalDate;

/**
 * Canonical VIOP contract metadata row.
 */
public record ViopContractRow(
        String contractCode,
        String underlying,
        String marketType,
        String marketGroup,
        LocalDate expiryDate,
        String settlementType,
        String currency,
        String sourceFile
) {}

