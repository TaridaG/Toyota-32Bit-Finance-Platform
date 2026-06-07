package com.company.marketdataservice.rates.infrastructure.http.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BondYieldCurveResponseDto {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate observationDate;
    private String sourceProvider = "TCMB_EVDS";
    private String unit = "PERCENT";
    private List<BondYieldCurvePointDto> points = new ArrayList<>();

    public LocalDate getObservationDate() {
        return observationDate;
    }

    public void setObservationDate(LocalDate observationDate) {
        this.observationDate = observationDate;
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

    public List<BondYieldCurvePointDto> getPoints() {
        return points;
    }

    public void setPoints(List<BondYieldCurvePointDto> points) {
        this.points = points;
    }
}
