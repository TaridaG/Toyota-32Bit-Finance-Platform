package com.company.analytics.domain;

import com.company.analytics.domain.enums.TrendDirection;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "analytics_trend_metric",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_analytics_trend_metric",
                        columnNames = {"instrument_id", "trade_date"}
                )
        }
)
@Getter
@Setter
public class AnalyticsTrendMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Column(name = "instrument_symbol", nullable = false, length = 32)
    private String instrumentSymbol;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "trend_direction", nullable = false, length = 16)
    private TrendDirection trendDirection;

    @Column(name = "momentum", precision = 19, scale = 8)
    private BigDecimal momentum;

    @Column(name = "price_slope", precision = 19, scale = 8)
    private BigDecimal priceSlope;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static AnalyticsTrendMetric create(
            Long instrumentId,
            String instrumentSymbol,
            LocalDate tradeDate
    ) {
        AnalyticsTrendMetric metric = new AnalyticsTrendMetric();
        Instant now = Instant.now();

        metric.setInstrumentId(instrumentId);
        metric.setInstrumentSymbol(instrumentSymbol);
        metric.setTradeDate(tradeDate);
        metric.setTrendDirection(TrendDirection.NEUTRAL);
        metric.setCreatedAt(now);
        metric.setUpdatedAt(now);

        return metric;
    }

    public void update(
            TrendDirection trendDirection,
            BigDecimal momentum,
            BigDecimal priceSlope
    ) {
        this.trendDirection = trendDirection;
        this.momentum = momentum;
        this.priceSlope = priceSlope;
        this.updatedAt = Instant.now();
    }
}