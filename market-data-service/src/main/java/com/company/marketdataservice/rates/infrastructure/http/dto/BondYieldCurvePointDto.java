package com.company.marketdataservice.rates.infrastructure.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BondYieldCurvePointDto {

    private String tenor;
    private String symbol;
    private BigDecimal value;
    private Integer change1dBasisPoints;

    public BondYieldCurvePointDto() {
    }

    public BondYieldCurvePointDto(String tenor, String symbol, BigDecimal value, Integer change1dBasisPoints) {
        this.tenor = tenor;
        this.symbol = symbol;
        this.value = value;
        this.change1dBasisPoints = change1dBasisPoints;
    }

    public String getTenor() {
        return tenor;
    }

    public void setTenor(String tenor) {
        this.tenor = tenor;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public Integer getChange1dBasisPoints() {
        return change1dBasisPoints;
    }

    public void setChange1dBasisPoints(Integer change1dBasisPoints) {
        this.change1dBasisPoints = change1dBasisPoints;
    }
}
