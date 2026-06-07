package com.company.marketdataservice.viop.domain;

import java.time.LocalDate;

/**
 * VIOP sözleşme kataloğu metadata satırını temsil eden domain katmanı tipi.
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

