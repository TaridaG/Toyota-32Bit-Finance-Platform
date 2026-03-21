package com.company.notification.event;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ReportFailedEvent {

    private UUID reportId;
    private String reportType;
    private String exportFormat;
    private String instrumentSymbol;
    private String reason;
    private UUID userId;
    private String userEmail;
    private Instant failedAt;
}