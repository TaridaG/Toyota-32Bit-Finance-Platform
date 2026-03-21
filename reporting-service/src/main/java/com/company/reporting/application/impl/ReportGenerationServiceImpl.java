package com.company.reporting.application.impl;

import com.company.reporting.application.ReportGenerationService;
import com.company.reporting.application.ReportRequestPublisher;
import com.company.reporting.domain.ReportMetadata;
import com.company.reporting.domain.enums.ReportType;
import com.company.reporting.dto.CreateInstrumentReportRequest;
import com.company.reporting.dto.CreatePortfolioReportRequest;
import com.company.reporting.dto.ReportMetadataResponse;
import com.company.reporting.event.ReportRequestedEvent;
import com.company.reporting.infrastructure.persistence.ReportMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportGenerationServiceImpl implements ReportGenerationService {

    private final ReportMetadataRepository reportMetadataRepository;
    private final ReportRequestPublisher reportRequestPublisher;

    @Override
    public ReportMetadataResponse generateInstrumentReport(CreateInstrumentReportRequest request) {
        ReportMetadata metadata = ReportMetadata.create(
                ReportType.INSTRUMENT,
                request.getExportFormat(),
                request.getSymbol()
        );

        reportMetadataRepository.save(metadata);

        ReportRequestedEvent event = new ReportRequestedEvent(
                metadata.getReportUuid(),
                ReportType.INSTRUMENT,
                request.getExportFormat(),
                request.getSymbol(),
                request.getFrom(),
                request.getTo(),
                request.getUserId(),
                request.getUserEmail()
        );

        reportRequestPublisher.publish(event);

        return ReportMetadataResponse.from(metadata);
    }

    @Override
    public ReportMetadataResponse generatePortfolioReport(CreatePortfolioReportRequest request) {
        ReportMetadata metadata = ReportMetadata.create(
                ReportType.PORTFOLIO,
                request.getExportFormat(),
                null
        );

        reportMetadataRepository.save(metadata);

        ReportRequestedEvent event = new ReportRequestedEvent(
                metadata.getReportUuid(),
                ReportType.PORTFOLIO,
                request.getExportFormat(),
                null,
                null,
                null,
                request.getUserId(),
                request.getUserEmail()
        );

        reportRequestPublisher.publish(event);

        return ReportMetadataResponse.from(metadata);
    }
}