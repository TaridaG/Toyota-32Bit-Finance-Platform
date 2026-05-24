package com.company.analytics.processing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Instrument için günlük moving average (MA7, MA30, MA90) değerlerini saklayan JPA entity. */
@Entity
@Table(name = "analytics_moving_average")
@Getter
@Setter
public class AnalyticsMovingAverage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long instrumentId;

    private String instrumentSymbol;

    private LocalDate tradeDate;

    private BigDecimal ma7;

    private BigDecimal ma30;

    private BigDecimal ma90;

    private Instant createdAt;

    private Instant updatedAt;

}
