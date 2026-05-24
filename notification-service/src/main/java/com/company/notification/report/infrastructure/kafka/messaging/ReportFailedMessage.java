package com.company.notification.report.infrastructure.kafka.messaging;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Kullanıcı rapor export'u başarısız olduğunda Kafka payload.
 */
@Data
public class ReportFailedMessage {

    private UUID reportId;
    private String reportType;
    private String exportFormat;
    private String instrumentSymbol;
    private String reason;
    private UUID userId;
    private String userEmail;
    private Instant failedAt;
}