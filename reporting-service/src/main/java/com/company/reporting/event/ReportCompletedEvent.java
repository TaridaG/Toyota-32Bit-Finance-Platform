package com.company.reporting.event;

import com.company.reporting.domain.enums.ExportFormat;
import com.company.reporting.domain.enums.ReportType;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ReportCompletedEvent {

    private final UUID reportId;
    private final ReportType reportType;
    private final ExportFormat exportFormat;
    private final String fileName;
    private final String fileKey;
    private final String instrumentSymbol;
    private final Instant completedAt;

    public ReportCompletedEvent(
            UUID reportId,
            ReportType reportType,
            ExportFormat exportFormat,
            String fileName,
            String fileKey,
            String instrumentSymbol,
            Instant completedAt
    ) {
        this.reportId = reportId;
        this.reportType = reportType;
        this.exportFormat = exportFormat;
        this.fileName = fileName;
        this.fileKey = fileKey;
        this.instrumentSymbol = instrumentSymbol;
        this.completedAt = completedAt;
    }
}