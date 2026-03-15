package com.company.reporting.application.impl;

import com.company.reporting.application.ReportProcessingService;
import com.company.reporting.application.exporter.ReportExporter;
import com.company.reporting.client.AnalyticsServiceClient;
import com.company.reporting.domain.ReportMetadata;
import com.company.reporting.dto.InstrumentReportData;
import com.company.reporting.event.ReportRequestedEvent;
import com.company.reporting.infrastructure.persistence.ReportMetadataRepository;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReportProcessingServiceImpl implements ReportProcessingService {

    private final AnalyticsServiceClient analyticsServiceClient;
    private final ReportMetadataRepository reportMetadataRepository;
    private final List<ReportExporter> exporters;
    private final Counter reportGeneratedCounter;

    @Override
    public void process(ReportRequestedEvent event) {
        ReportMetadata metadata = reportMetadataRepository.findByReportUuid(event.getReportId())
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + event.getReportId()));

        metadata.markProcessing();
        reportMetadataRepository.save(metadata);

        try {
            InstrumentReportData reportData = InstrumentReportData.builder()
                    .symbol(event.getSymbol())
                    .candles(analyticsServiceClient.getCandles(event.getSymbol(), event.getFrom(), event.getTo()))
                    .movingAverages(analyticsServiceClient.getMovingAverages(event.getSymbol()))
                    .trendMetrics(analyticsServiceClient.getTrendMetrics(event.getSymbol()))
                    .build();

            ReportExporter exporter = resolveExporter(event);
            byte[] content = exporter.export(reportData);
            String fileName = exporter.buildFileName(event.getSymbol());

            metadata.markCompleted(content, fileName);
            reportMetadataRepository.save(metadata);

            reportGeneratedCounter.increment();

            log.info("Report completed reportId={} symbol={} format={}",
                    event.getReportId(), event.getSymbol(), event.getExportFormat());
        } catch (Exception ex) {
            metadata.markFailed(ex.getMessage());
            reportMetadataRepository.save(metadata);

            log.error("Report processing failed reportId={} reason={}",
                    event.getReportId(), ex.getMessage(), ex);

            throw ex;
        }
    }

    private ReportExporter resolveExporter(ReportRequestedEvent event) {
        return exporters.stream()
                .filter(exporter -> exporter.supports() == event.getExportFormat())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No exporter found for format: " + event.getExportFormat()
                ));
    }
}