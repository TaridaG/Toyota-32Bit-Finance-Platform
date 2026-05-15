package com.company.marketdataservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TlDepositLatestDto {

    private BigDecimal value;
    /** Prior ISO week level for the same maturity (nullable if unknown). */
    private BigDecimal previousValue;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate asOfDate;
    /** {@code UNCHANGED}, {@code UP}, or {@code DOWN} vs prior week's stored level. */
    private String changeVsPrior;
    /** EVDS maturity bucket, e.g. {@code MT04} (≤1Y). */
    private String maturityCode;
    private String sourceProvider = "TCMB_EVDS";
    private String unit = "PERCENT";

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(BigDecimal previousValue) {
        this.previousValue = previousValue;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(LocalDate asOfDate) {
        this.asOfDate = asOfDate;
    }

    public String getChangeVsPrior() {
        return changeVsPrior;
    }

    public void setChangeVsPrior(String changeVsPrior) {
        this.changeVsPrior = changeVsPrior;
    }

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
}
