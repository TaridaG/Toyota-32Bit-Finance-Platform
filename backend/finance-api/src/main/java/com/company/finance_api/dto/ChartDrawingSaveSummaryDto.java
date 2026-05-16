package com.company.finance_api.dto;

import java.time.Instant;
import java.util.List;

public class ChartDrawingSaveSummaryDto {

    private final Long id;
    private final String name;
    private final String assetKey;
    private final String assetSymbol;
    private final String assetType;
    private final Instant createdAt;
    private final List<String> drawingTypes;
    private final List<DrawingMarkerDto> drawingMarkers;
    private final Long minAnchorTime;
    private final Long maxAnchorTime;
    private final Double minPrice;
    private final Double maxPrice;

    public ChartDrawingSaveSummaryDto(
            Long id,
            String name,
            String assetKey,
            String assetSymbol,
            String assetType,
            Instant createdAt,
            List<String> drawingTypes,
            List<DrawingMarkerDto> drawingMarkers,
            Long minAnchorTime,
            Long maxAnchorTime,
            Double minPrice,
            Double maxPrice
    ) {
        this.id = id;
        this.name = name;
        this.assetKey = assetKey;
        this.assetSymbol = assetSymbol;
        this.assetType = assetType;
        this.createdAt = createdAt;
        this.drawingTypes = drawingTypes;
        this.drawingMarkers = drawingMarkers;
        this.minAnchorTime = minAnchorTime;
        this.maxAnchorTime = maxAnchorTime;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAssetKey() {
        return assetKey;
    }

    public String getAssetSymbol() {
        return assetSymbol;
    }

    public String getAssetType() {
        return assetType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<String> getDrawingTypes() {
        return drawingTypes;
    }

    public List<DrawingMarkerDto> getDrawingMarkers() {
        return drawingMarkers;
    }

    public Long getMinAnchorTime() {
        return minAnchorTime;
    }

    public Long getMaxAnchorTime() {
        return maxAnchorTime;
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }
}
