package com.company.notification.alarm.infrastructure.kafka.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code alarm-triggered} Kafka topic'i için deserialize edilmiş payload (finance-api tarafından publish edilir).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlarmTriggeredMessage {

    private Long alarmId;
    private UUID userId;
    private String userEmail;
    private String preferredLocale;
    private String instrumentSymbol;
    private String condition;
    private BigDecimal threshold;
    private BigDecimal price;
    private Instant triggeredAt;
}
