package com.company.marketdataservice.history;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "mds_market_price_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_mds_market_price_history_symbol_provider_time_type",
                        columnNames = {"instrument_symbol", "provider", "observed_at", "price_type"}
                ),
                @UniqueConstraint(
                        name = "uk_mds_market_price_history_event_id",
                        columnNames = {"event_id"}
                )
        },
        indexes = {
                @Index(name = "idx_mds_market_price_history_instrument_time", columnList = "instrument_id, observed_at"),
                @Index(name = "idx_mds_market_price_history_symbol_time", columnList = "instrument_symbol, observed_at"),
                @Index(name = "idx_mds_market_price_history_provider_time", columnList = "provider, observed_at")
        }
)
@Getter
@Setter
public class MarketPriceHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrument_id")
    private Long instrumentId;

    @Column(name = "instrument_symbol", nullable = false, length = 128)
    private String instrumentSymbol;

    @Column(nullable = false, length = 64)
    private String provider;

    @Column(name = "source_symbol", length = 128)
    private String sourceSymbol;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal price;

    @Column(name = "price_type", nullable = false, length = 64)
    private String priceType;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "ingest_time", nullable = false)
    private Instant ingestTime;
}
