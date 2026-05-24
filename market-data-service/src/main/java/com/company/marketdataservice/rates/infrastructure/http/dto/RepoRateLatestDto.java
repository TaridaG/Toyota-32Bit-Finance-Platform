package com.company.marketdataservice.rates.infrastructure.http.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepoRateLatestDto {

    private BigDecimal value;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate observationDate;
    private Integer change1dBasisPoints;
    private String sourceProvider = "TCMB_EVDS";
    private String unit = "PERCENT";
    private String evdsSeries;

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public LocalDate getObservationDate() {
        return observationDate;
    }

    public void setObservationDate(LocalDate observationDate) {
        this.observationDate = observationDate;
    }

    public Integer getChange1dBasisPoints() {
        return change1dBasisPoints;
    }

    public void setChange1dBasisPoints(Integer change1dBasisPoints) {
        this.change1dBasisPoints = change1dBasisPoints;
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

    public String getEvdsSeries() {
        return evdsSeries;
    }

    public void setEvdsSeries(String evdsSeries) {
        this.evdsSeries = evdsSeries;
    }
}
