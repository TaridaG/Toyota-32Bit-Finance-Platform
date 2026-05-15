package com.company.marketdataservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolicyRateLatestDto {

    private BigDecimal value;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate decisionDate;
    /** {@code UNCHANGED}, {@code UP}, or {@code DOWN} vs prior distinct level. */
    private String changeVsPrior;
    private String sourceProvider = "TCMB_EVDS";
    private String unit = "PERCENT";

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public LocalDate getDecisionDate() {
        return decisionDate;
    }

    public void setDecisionDate(LocalDate decisionDate) {
        this.decisionDate = decisionDate;
    }

    public String getChangeVsPrior() {
        return changeVsPrior;
    }

    public void setChangeVsPrior(String changeVsPrior) {
        this.changeVsPrior = changeVsPrior;
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
