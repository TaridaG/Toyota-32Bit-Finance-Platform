package com.company.marketdataservice.spot.infrastructure.http.dto;

/**
 * `spot fiyat` REST API için HTTP DTO.
 */
public record MarketSegmentPulseRowDto(
        String segment,
        Double meanChange1D,
        int count,
        int advancingCount
) {}
