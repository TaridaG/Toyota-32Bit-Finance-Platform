package com.company.marketdataservice.spot.infrastructure.http.dto;
import java.time.Instant;
import java.util.List;

/**
 * `spot fiyat` infrastructure katmanı adaptörü.
 */
public record MarketSegmentPulseResponse(
        MarketSegmentPulseOverallDto overall, List<MarketSegmentPulseRowDto> segments, Instant generatedAt) {}
