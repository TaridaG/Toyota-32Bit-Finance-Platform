package com.company.marketdataservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolicyRateHistoryPointDto {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private BigDecimal value;
    private PolicyRatePointSourceQuality sourceQuality;

    public PolicyRateHistoryPointDto() {
    }

    public PolicyRateHistoryPointDto(LocalDate date, BigDecimal value) {
        this.date = date;
        this.value = value;
    }

    public PolicyRateHistoryPointDto(LocalDate date, BigDecimal value, PolicyRatePointSourceQuality sourceQuality) {
        this.date = date;
        this.value = value;
        this.sourceQuality = sourceQuality;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public PolicyRatePointSourceQuality getSourceQuality() {
        return sourceQuality;
    }

    public void setSourceQuality(PolicyRatePointSourceQuality sourceQuality) {
        this.sourceQuality = sourceQuality;
    }
}
