package com.company.analytics.processing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Idempotency kontrolü için daha önce işlenmiş Kafka event kayıtlarını saklayan JPA entity. */
@Entity
@Table(name = "analytics_processed_events")
@Getter
@Setter
public class ProcessedAnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String eventKey;

    private Instant processedAt;

}
