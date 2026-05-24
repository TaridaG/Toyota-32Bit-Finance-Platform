package com.company.finance_api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;

/** ChartDrawingSaveDetailDto — API transfer nesnesi (DTO/response/request). */
public class ChartDrawingSaveDetailDto {

  private final Long id;
  private final String name;
  private final String assetKey;
  private final String assetSymbol;
  private final String assetType;
  private final Instant createdAt;
  private final int drawingCount;
  private final List<String> drawingTypes;
  private final List<DrawingMarkerDto> drawingMarkers;
  private final Long minAnchorTime;
  private final Long maxAnchorTime;
  private final Double minPrice;
  private final Double maxPrice;
  private final JsonNode drawings;

  public ChartDrawingSaveDetailDto(
      Long id,
      String name,
      String assetKey,
      String assetSymbol,
      String assetType,
      Instant createdAt,
      int drawingCount,
      List<String> drawingTypes,
      List<DrawingMarkerDto> drawingMarkers,
      Long minAnchorTime,
      Long maxAnchorTime,
      Double minPrice,
      Double maxPrice,
      JsonNode drawings) {
    this.id = id;
    this.name = name;
    this.assetKey = assetKey;
    this.assetSymbol = assetSymbol;
    this.assetType = assetType;
    this.createdAt = createdAt;
    this.drawingCount = drawingCount;
    this.drawingTypes = drawingTypes;
    this.drawingMarkers = drawingMarkers;
    this.minAnchorTime = minAnchorTime;
    this.maxAnchorTime = maxAnchorTime;
    this.minPrice = minPrice;
    this.maxPrice = maxPrice;
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

  public int getDrawingCount() {
    return drawingCount;
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

  public JsonNode getDrawings() {
    return drawings;
  }
}
