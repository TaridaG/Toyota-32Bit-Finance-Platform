package com.company.marketdataservice.dto;

/**
 * One segment strip card: equal-weight mean of 1-day % moves for instruments in that UI bucket.
 */
public record MarketSegmentPulseRowDto(
        String segment,
        Double meanChange1D,
        int count,
        int advancingCount
) {}
