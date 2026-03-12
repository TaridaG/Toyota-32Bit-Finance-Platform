package com.company.reporting.application.exporter;

import com.company.reporting.domain.enums.ExportFormat;
import com.company.reporting.dto.InstrumentReportData;

public interface ReportExporter {

    ExportFormat supports();

    byte[] export(InstrumentReportData data);

    String buildFileName(String symbol);
}