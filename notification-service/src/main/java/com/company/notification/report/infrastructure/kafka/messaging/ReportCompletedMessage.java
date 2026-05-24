package com.company.notification.report.infrastructure.kafka.messaging;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Kullanıcı rapor export'u başarıyla tamamlandığında Kafka payload.
 */
@Data
public class ReportCompletedMessage {

    private UUID reportId;
    private String reportType;
    private String exportFormat;
    private String fileName;
    private String fileKey;
    private String instrumentSymbol;
    private UUID userId;
    private String userEmail;
    private Instant completedAt;
}