package com.company.reporting.application.impl;

import com.company.reporting.application.ReportGenerationService;
import com.company.reporting.application.exporter.ReportExporter;
import com.company.reporting.client.AnalyticsServiceClient;
import com.company.reporting.domain.ReportMetadata;
import com.company.reporting.domain.enums.ReportType;
import com.company.reporting.dto.CreateInstrumentReportRequest;
import com.company.reporting.dto.InstrumentReportData;
import com.company.reporting.dto.ReportMetadataResponse;
import com.company.reporting.infrastructure.persistence.ReportMetadataRepository;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportGenerationServiceImpl implements ReportGenerationService {

    private final AnalyticsServiceClient analyticsServiceClient;
    private final ReportMetadataRepository reportMetadataRepository;
    private final List<ReportExporter> exporters;
    private final Counter reportGeneratedCounter;

    @Override
    public ReportMetadataResponse generateInstrumentReport(CreateInstrumentReportRequest request) {
        ReportMetadata metadata = ReportMetadata.create(
                ReportType.INSTRUMENT,
                request.getExportFormat(),
                request.getSymbol()
        );

        reportMetadataRepository.save(metadata);

        try {
            InstrumentReportData reportData = InstrumentReportData.builder()
                    .symbol(request.getSymbol())
                    .candles(analyticsServiceClient.getCandles(request.getSymbol(), request.getFrom(), request.getTo()))
                    .movingAverages(analyticsServiceClient.getMovingAverages(request.getSymbol()))
                    .trendMetrics(analyticsServiceClient.getTrendMetrics(request.getSymbol()))
                    .build();

            ReportExporter exporter = resolveExporter(request.getExportFormat());
            byte[] content = exporter.export(reportData);
            String fileName = exporter.buildFileName(request.getSymbol());

            metadata.markCompleted(content, fileName);
            reportMetadataRepository.save(metadata);
            reportGeneratedCounter.increment();

            return ReportMetadataResponse.from(metadata);
        } catch (Exception e) {
            metadata.markFailed(e.getMessage());
            reportMetadataRepository.save(metadata);
            throw e;
        }
    }

    private ReportExporter resolveExporter(com.company.reporting.domain.enums.ExportFormat exportFormat) {
        return exporters.stream()
                .filter(exporter -> exporter.supports() == exportFormat)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No exporter found for format: " + exportFormat));
    }
}