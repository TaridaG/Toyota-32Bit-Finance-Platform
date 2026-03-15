package com.company.reporting.event;

import com.company.reporting.domain.enums.ExportFormat;
import com.company.reporting.domain.enums.ReportType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ReportRequestedEvent {

    private UUID reportId;
    private ReportType reportType;
    private ExportFormat exportFormat;
    private String symbol;
    private LocalDate from;
    private LocalDate to;

    public ReportRequestedEvent(
            UUID reportId,
            ReportType reportType,
            ExportFormat exportFormat,
            String symbol,
            LocalDate from,
            LocalDate to
    ) {
        this.reportId = reportId;
        this.reportType = reportType;
        this.exportFormat = exportFormat;
        this.symbol = symbol;
        this.from = from;
        this.to = to;
    }
}