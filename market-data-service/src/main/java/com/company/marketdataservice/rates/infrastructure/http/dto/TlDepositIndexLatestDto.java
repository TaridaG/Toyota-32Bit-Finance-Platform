package com.company.marketdataservice.rates.infrastructure.http.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TlDepositIndexLatestDto {

  private String maturityCode;
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate asOfDate;
  private BigDecimal indexValue;
  private BigDecimal annualRatePercent;
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate sourceObservationDate;
  private String sourceProvider = "TCMB_EVDS";
  private String unit = "INDEX";

  public String getMaturityCode() {
    return maturityCode;
  }

  public void setMaturityCode(String maturityCode) {
    this.maturityCode = maturityCode;
  }

  public LocalDate getAsOfDate() {
    return asOfDate;
  }

  public void setAsOfDate(LocalDate asOfDate) {
    this.asOfDate = asOfDate;
  }

  public BigDecimal getIndexValue() {
    return indexValue;
  }

  public void setIndexValue(BigDecimal indexValue) {
    this.indexValue = indexValue;
  }

  public BigDecimal getAnnualRatePercent() {
    return annualRatePercent;
  }

  public void setAnnualRatePercent(BigDecimal annualRatePercent) {
    this.annualRatePercent = annualRatePercent;
  }

  public LocalDate getSourceObservationDate() {
    return sourceObservationDate;
  }

  public void setSourceObservationDate(LocalDate sourceObservationDate) {
    this.sourceObservationDate = sourceObservationDate;
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
}
