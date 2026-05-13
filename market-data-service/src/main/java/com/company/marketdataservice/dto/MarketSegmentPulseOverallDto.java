package com.company.marketdataservice.dto;

/** Equal-weight 1D % move across every instrument included in the segment pulse universe. */
public record MarketSegmentPulseOverallDto(Double meanChange1D, int count, int advancingCount) {}
