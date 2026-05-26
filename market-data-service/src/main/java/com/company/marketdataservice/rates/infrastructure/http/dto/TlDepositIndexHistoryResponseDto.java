package com.company.marketdataservice.rates.infrastructure.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TlDepositIndexHistoryResponseDto {

  private String maturityCode;
  private String sourceProvider = "TCMB_EVDS";
  private String unit = "INDEX";
  private List<TlDepositIndexPointDto> points = new ArrayList<>();

  public String getMaturityCode() {
    return maturityCode;
  }

  public void setMaturityCode(String maturityCode) {
    this.maturityCode = maturityCode;
  }

  public String getSourceProvider() {
    return sourceProvider;
  }

  public void setSourceProvider(String sourceProvider) {
    this.sourceProvider = sourceProvider;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public List<TlDepositIndexPointDto> getPoints() {
    return points;
  }

  public void setPoints(List<TlDepositIndexPointDto> points) {
    this.points = points;
  }
}
