package com.company.reporting.application;

import com.company.reporting.dto.ReportContentResponse;
import com.company.reporting.dto.ReportMetadataResponse;

import java.util.UUID;

public interface ReportQueryService {

    ReportMetadataResponse getReportMetadata(UUID reportId);

    ReportContentResponse download(UUID reportId);
}