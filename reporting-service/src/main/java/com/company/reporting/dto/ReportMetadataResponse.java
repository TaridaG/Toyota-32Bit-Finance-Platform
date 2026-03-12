package com.company.reporting.dto;

import com.company.reporting.domain.ReportMetadata;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ReportMetadataResponse {

    private UUID reportId;
    private String status;
    private String fileName;
    private String reportType;
    private String exportFormat;
    private String instrumentSymbol;

    public static ReportMetadataResponse from(ReportMetadata metadata) {
        return ReportMetadataResponse.builder()
                .reportId(metadata.getReportUuid())
                .status(metadata.getStatus().name())
                .fileName(metadata.getGeneratedFileName())
                .reportType(metadata.getReportType().name())
                .exportFormat(metadata.getExportFormat().name())
                .instrumentSymbol(metadata.getInstrumentSymbol())
                .build();
    }
}