package com.company.finance_api.dto;

public class DrawingMarkerDto {

    private final String type;
    private final String color;

    public DrawingMarkerDto(String type, String color) {
        this.type = type;
        this.color = color;
    }

    public String getType() {
        return type;
    }

    public String getColor() {
        return color;
    }
}
