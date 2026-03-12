package com.company.reporting.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportContentResponse {

    private String fileName;
    private String contentType;
    private byte[] content;
}