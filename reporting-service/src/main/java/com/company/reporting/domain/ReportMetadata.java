package com.company.reporting.domain;

import com.company.reporting.domain.enums.ExportFormat;
import com.company.reporting.domain.enums.ReportStatus;
import com.company.reporting.domain.enums.ReportType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "report_metadata")
@Getter
@Setter
public class ReportMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_uuid", nullable = false, unique = true)
    private UUID reportUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 32)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_format", nullable = false, length = 16)
    private ExportFormat exportFormat;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ReportStatus status;

    @Column(name = "instrument_symbol", length = 32)
    private String instrumentSymbol;

    @Column(name = "file_key")
    private String fileKey;

    @Column(name = "generated_file_name", length = 255)
    private String generatedFileName;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static ReportMetadata create(
            ReportType reportType,
            ExportFormat exportFormat,
            String instrumentSymbol
    ) {
        ReportMetadata metadata = new ReportMetadata();
        Instant now = Instant.now();

        metadata.setReportUuid(UUID.randomUUID());
        metadata.setReportType(reportType);
        metadata.setExportFormat(exportFormat);
        metadata.setStatus(ReportStatus.QUEUED);
        metadata.setInstrumentSymbol(instrumentSymbol);
        metadata.setCreatedAt(now);
        metadata.setUpdatedAt(now);

        return metadata;
    }

    public void markProcessing() {
        this.status = ReportStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markCompleted(String fileKey, String generatedFileName) {
        this.fileKey = fileKey;
        this.generatedFileName = generatedFileName;
        this.status = ReportStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.failureReason = reason;
        this.status = ReportStatus.FAILED;
        this.updatedAt = Instant.now();
    }
}