package com.company.reporting.application.impl;

import com.company.reporting.application.ReportProcessingService;
import com.company.reporting.application.ReportStatusEventPublisher;
import com.company.reporting.application.exporter.CsvInstrumentReportExporter;
import com.company.reporting.application.exporter.CsvPortfolioReportExporter;
import com.company.reporting.application.exporter.PdfInstrumentReportExporter;
import com.company.reporting.application.exporter.PdfPortfolioReportExporter;
import com.company.reporting.application.portfolio.PortfolioReportBuilder;
import com.company.reporting.client.AnalyticsServiceClient;
import com.company.reporting.client.FinanceServiceClient;
import com.company.reporting.domain.ReportMetadata;
import com.company.reporting.domain.enums.ExportFormat;
import com.company.reporting.domain.enums.ReportType;
import com.company.reporting.dto.InstrumentReportData;
import com.company.reporting.dto.PortfolioReportData;
import com.company.reporting.event.ReportCompletedEvent;
import com.company.reporting.event.ReportFailedEvent;
import com.company.reporting.event.ReportRequestedEvent;
import com.company.reporting.infrastructure.persistence.ReportMetadataRepository;
import com.company.reporting.storage.FileStorageService;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReportProcessingServiceImpl implements ReportProcessingService {

    private final AnalyticsServiceClient analyticsServiceClient;
    private final FinanceServiceClient financeServiceClient;
    private final PortfolioReportBuilder portfolioReportBuilder;
    private final ReportMetadataRepository reportMetadataRepository;
    private final CsvInstrumentReportExporter csvInstrumentReportExporter;
    private final PdfInstrumentReportExporter pdfInstrumentReportExporter;
    private final CsvPortfolioReportExporter csvPortfolioReportExporter;
    private final PdfPortfolioReportExporter pdfPortfolioReportExporter;
    private final FileStorageService fileStorageService;
    private final Counter reportGeneratedCounter;
    private final ReportStatusEventPublisher reportStatusEventPublisher;

    @Override
    public void process(ReportRequestedEvent event) {
        ReportMetadata metadata = reportMetadataRepository.findByReportUuid(event.getReportId())
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + event.getReportId()));

        metadata.markProcessing();
        reportMetadataRepository.save(metadata);

        try {
            if (event.getReportType() == ReportType.INSTRUMENT) {
                processInstrumentReport(event, metadata);
            } else if (event.getReportType() == ReportType.PORTFOLIO) {
                processPortfolioReport(event, metadata);
            } else {
                throw new IllegalStateException("Unsupported report type: " + event.getReportType());
            }

            reportGeneratedCounter.increment();
            reportStatusEventPublisher.publishCompleted(
                    new ReportCompletedEvent(
                            metadata.getReportUuid(),
                            metadata.getReportType(),
                            metadata.getExportFormat(),
                            metadata.getGeneratedFileName(),
                            metadata.getFileKey(),
                            metadata.getInstrumentSymbol(),
                            event.getUserId(),
                            event.getUserEmail(),
                            Instant.now()
                    )
            );

            log.info("Report completed reportId={} type={} format={}",
                    event.getReportId(), event.getReportType(), event.getExportFormat());
        } catch (Exception ex) {
            metadata.markFailed(ex.getMessage());
            reportMetadataRepository.save(metadata);
            reportStatusEventPublisher.publishFailed(
                    new ReportFailedEvent(
                            metadata.getReportUuid(),
                            metadata.getReportType(),
                            metadata.getExportFormat(),
                            metadata.getInstrumentSymbol(),
                            ex.getMessage(),
                            event.getUserId(),
                            event.getUserEmail(),
                            Instant.now()
                    )
            );

            log.error("Report processing failed reportId={} reason={}",
                    event.getReportId(), ex.getMessage(), ex);

            throw ex;
        }
    }

    private void processInstrumentReport(ReportRequestedEvent event, ReportMetadata metadata) {
        InstrumentReportData reportData = InstrumentReportData.builder()
                .symbol(event.getSymbol())
                .candles(analyticsServiceClient.getCandles(event.getSymbol(), event.getFrom(), event.getTo()))
                .movingAverages(analyticsServiceClient.getMovingAverages(event.getSymbol()))
                .trendMetrics(analyticsServiceClient.getTrendMetrics(event.getSymbol()))
                .build();

        byte[] content;
        String fileName;

        if (event.getExportFormat() == ExportFormat.CSV) {
            content = csvInstrumentReportExporter.export(reportData);
            fileName = csvInstrumentReportExporter.buildFileName(event.getSymbol());
        } else {
            content = pdfInstrumentReportExporter.export(reportData);
            fileName = pdfInstrumentReportExporter.buildFileName(event.getSymbol());
        }

        String contentType = event.getExportFormat() == ExportFormat.CSV ? "text/csv" : "application/pdf";
        String fileKey = fileStorageService.upload(fileName, content, contentType);

        metadata.markCompleted(fileKey, fileName);
        reportMetadataRepository.save(metadata);
    }

    private void processPortfolioReport(ReportRequestedEvent event, ReportMetadata metadata) {
        PortfolioReportData reportData = portfolioReportBuilder.build(
                financeServiceClient.getPortfolioAssets()
        );

        byte[] content;
        String fileName;

        if (event.getExportFormat() == ExportFormat.CSV) {
            content = csvPortfolioReportExporter.export(reportData);
            fileName = csvPortfolioReportExporter.buildFileName();
        } else {
            content = pdfPortfolioReportExporter.export(reportData);
            fileName = pdfPortfolioReportExporter.buildFileName();
        }

        String contentType = event.getExportFormat() == ExportFormat.CSV ? "text/csv" : "application/pdf";
        String fileKey = fileStorageService.upload(fileName, content, contentType);

        metadata.markCompleted(fileKey, fileName);
        reportMetadataRepository.save(metadata);
    }
}