package com.company.marketdataservice.history;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "mds_fx_rate_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_mds_fx_rate_history_symbol_provider_time",
                        columnNames = {"canonical_symbol", "provider", "observed_at"}
                ),
                @UniqueConstraint(
                        name = "uk_mds_fx_rate_history_event_id",
                        columnNames = {"event_id"}
                )
        },
        indexes = {
                @Index(name = "idx_mds_fx_rate_history_instrument_time", columnList = "instrument_id, observed_at"),
                @Index(name = "idx_mds_fx_rate_history_symbol_time", columnList = "canonical_symbol, observed_at")
        }
)
@Getter
@Setter
public class FxRateHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrument_id")
    private Long instrumentId;

    @Column(name = "canonical_symbol", nullable = false, length = 64)
    private String canonicalSymbol;

    @Column(name = "base_currency", nullable = false, length = 16)
    private String baseCurrency;

    @Column(name = "quote_currency", nullable = false, length = 16)
    private String quoteCurrency;

    @Column(precision = 24, scale = 8)
    private BigDecimal bid;

    @Column(precision = 24, scale = 8)
    private BigDecimal ask;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal mid;

    @Column(nullable = false, length = 64)
    private String provider;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "ingest_time", nullable = false)
    private Instant ingestTime;
}
