package com.company.reporting.application.impl;

import com.company.reporting.application.ReportQueryService;
import com.company.reporting.domain.ReportMetadata;
import com.company.reporting.domain.enums.ExportFormat;
import com.company.reporting.dto.ReportContentResponse;
import com.company.reporting.dto.ReportMetadataResponse;
import com.company.reporting.infrastructure.persistence.ReportMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportQueryServiceImpl implements ReportQueryService {

    private final ReportMetadataRepository repository;


    @Override
    public ReportMetadataResponse getReportMetadata(UUID reportId) {
        ReportMetadata metadata = repository.findByReportUuid(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        return ReportMetadataResponse.from(metadata);
    }

    @Override
    public ReportContentResponse download(UUID reportId) {
        ReportMetadata metadata = repository.findByReportUuid(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        if (metadata.getContent() == null || metadata.getContent().length == 0) {
            throw new IllegalArgumentException("Report content is not ready yet");
        }

        String mediaType = resolveMediaType(metadata.getExportFormat());

        return ReportContentResponse.builder()
                .fileName(metadata.getGeneratedFileName())
                .mediaType(mediaType)
                .content(metadata.getContent())
                .build();
    }
    private String resolveMediaType(ExportFormat exportFormat) {
        if (exportFormat == ExportFormat.PDF) {
            return "application/pdf";
        }

        if (exportFormat == ExportFormat.CSV) {
            return "text/csv";
        }

        return "application/octet-stream";
    }
}