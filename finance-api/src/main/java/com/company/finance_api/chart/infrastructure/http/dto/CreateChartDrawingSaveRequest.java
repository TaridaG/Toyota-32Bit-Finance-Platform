package com.company.finance_api.chart.infrastructure.http.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** CreateChartDrawingSaveRequest — API transfer nesnesi (DTO/response/request). */
public class CreateChartDrawingSaveRequest {

  @NotBlank
  @Size(max = 64)
  private String assetKey;

  @NotBlank
  @Size(max = 32)
  private String assetSymbol;

  @Size(max = 24)
  private String assetType;

  @NotBlank
  @Size(max = 120)
  private String name;

  @NotNull private JsonNode drawings;

  public String getAssetKey() {
    return assetKey;
  }

  public void setAssetKey(String assetKey) {
    this.assetKey = assetKey;
  }

  public String getAssetSymbol() {
    return assetSymbol;
  }

  public void setAssetSymbol(String assetSymbol) {
    this.assetSymbol = assetSymbol;
  }

  public String getAssetType() {
    return assetType;
  }

  public void setAssetType(String assetType) {
    this.assetType = assetType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public JsonNode getDrawings() {
    return drawings;
  }

  public void setDrawings(JsonNode drawings) {
    this.drawings = drawings;
  }
}
