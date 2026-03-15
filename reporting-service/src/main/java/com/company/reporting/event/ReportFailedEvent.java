package com.company.reporting.event;

import com.company.reporting.domain.enums.ExportFormat;
import com.company.reporting.domain.enums.ReportType;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ReportFailedEvent {

    private final UUID reportId;
    private final ReportType reportType;
    private final ExportFormat exportFormat;
    private final String instrumentSymbol;
    private final String reason;
    private final Instant failedAt;

    public ReportFailedEvent(
            UUID reportId,
            ReportType reportType,
            ExportFormat exportFormat,
            String instrumentSymbol,
            String reason,
            Instant failedAt
    ) {
        this.reportId = reportId;
        this.reportType = reportType;
        this.exportFormat = exportFormat;
        this.instrumentSymbol = instrumentSymbol;
        this.reason = reason;
        this.failedAt = failedAt;
    }
}