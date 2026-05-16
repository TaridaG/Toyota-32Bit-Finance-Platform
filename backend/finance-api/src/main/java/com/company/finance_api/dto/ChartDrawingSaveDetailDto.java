package com.company.finance_api.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

public class ChartDrawingSaveDetailDto {

    private final Long id;
    private final String name;
    private final String assetKey;
    private final String assetSymbol;
    private final String assetType;
    private final Instant createdAt;
    private final List<String> drawingTypes;
    private final List<DrawingMarkerDto> drawingMarkers;
    private final JsonNode drawings;

    public ChartDrawingSaveDetailDto(
            Long id,
            String name,
            String assetKey,
            String assetSymbol,
            String assetType,
            Instant createdAt,
            List<String> drawingTypes,
            List<DrawingMarkerDto> drawingMarkers,
            JsonNode drawings
    ) {
        this.id = id;
        this.name = name;
        this.assetKey = assetKey;
        this.assetSymbol = assetSymbol;
        this.assetType = assetType;
        this.createdAt = createdAt;
        this.drawingTypes = drawingTypes;
        this.drawingMarkers = drawingMarkers;
        this.drawings = drawings;
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

    public JsonNode getDrawings() {
        return drawings;
    }
}
