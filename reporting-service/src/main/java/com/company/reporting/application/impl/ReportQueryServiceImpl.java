package com.company.reporting.application.impl;

import com.company.reporting.application.ReportQueryService;
import com.company.reporting.domain.ReportMetadata;
import com.company.reporting.domain.enums.ExportFormat;
import com.company.reporting.dto.ReportContentResponse;
import com.company.reporting.dto.ReportMetadataResponse;
import com.company.reporting.infrastructure.persistence.ReportMetadataRepository;
import com.company.reporting.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportQueryServiceImpl implements ReportQueryService {

    private final ReportMetadataRepository repository;
    private final FileStorageService fileStorageService;


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

        if (metadata.getFileKey() == null || metadata.getFileKey().isBlank()) {
            throw new IllegalArgumentException("Report content is not ready yet");
        }

        byte[] content = fileStorageService.download(metadata.getFileKey());
        String mediaType = resolveMediaType(metadata.getExportFormat());

        return ReportContentResponse.builder()
                .fileName(metadata.getGeneratedFileName())
                .mediaType(mediaType)
                .content(content)
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