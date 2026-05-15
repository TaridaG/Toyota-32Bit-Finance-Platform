package com.company.marketdataservice.dto;

import java.util.ArrayList;
import java.util.List;

public class PolicyRateHistoryResponseDto {

    private String symbol = "TR_POLICY_RATE";
    private String name = "TCMB Politika Faizi";
    private String sourceProvider = "TCMB_EVDS";
    private String frequency = "WEEKLY";
    private String unit = "PERCENT";
    private List<PolicyRateHistoryPointDto> points = new ArrayList<>();

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceProvider() {
        return sourceProvider;
    }

    public void setSourceProvider(String sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public List<PolicyRateHistoryPointDto> getPoints() {
        return points;
    }

    public void setPoints(List<PolicyRateHistoryPointDto> points) {
        this.points = points;
    }
}
