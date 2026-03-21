package com.company.notification.event;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ReportCompletedEvent {

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