package com.company.reporting.application;

import com.company.reporting.dto.CreateInstrumentReportRequest;
import com.company.reporting.dto.ReportMetadataResponse;

public interface ReportGenerationService {

    ReportMetadataResponse generateInstrumentReport(CreateInstrumentReportRequest request);
}