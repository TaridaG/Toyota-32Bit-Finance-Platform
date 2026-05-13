package com.company.marketdataservice.dto;

import java.time.Instant;
import java.util.List;

public record MarketSegmentPulseResponse(
        MarketSegmentPulseOverallDto overall, List<MarketSegmentPulseRowDto> segments, Instant generatedAt) {}
