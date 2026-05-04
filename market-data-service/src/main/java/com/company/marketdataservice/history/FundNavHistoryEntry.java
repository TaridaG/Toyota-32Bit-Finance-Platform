package com.company.marketdataservice.history;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "mds_fund_nav_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_mds_fund_nav_history_code_provider_time",
                        columnNames = {"fund_code", "provider", "observed_at"}
                ),
                @UniqueConstraint(
                        name = "uk_mds_fund_nav_history_event_id",
                        columnNames = {"event_id"}
                )
        },
        indexes = {
                @Index(name = "idx_mds_fund_nav_history_instrument_time", columnList = "instrument_id, observed_at"),
                @Index(name = "idx_mds_fund_nav_history_code_time", columnList = "fund_code, observed_at")
        }
)
@Getter
@Setter
public class FundNavHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrument_id")
    private Long instrumentId;

    @Column(name = "fund_code", nullable = false, length = 64)
    private String fundCode;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal nav;

    @Column(nullable = false, length = 64)
    private String provider;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "ingest_time", nullable = false)
    private Instant ingestTime;
}
