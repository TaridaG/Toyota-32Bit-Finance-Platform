package com.company.marketdataservice.spot.infrastructure.http.dto;

/**
 * `spot fiyat` REST API için HTTP DTO.
 */
public record MarketSegmentPulseOverallDto(Double meanChange1D, int count, int advancingCount) {}
