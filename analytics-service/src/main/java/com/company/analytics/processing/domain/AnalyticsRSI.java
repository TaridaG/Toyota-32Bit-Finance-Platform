package com.company.analytics.processing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Instrument için günlük RSI-14 (Relative Strength Index) değerini saklayan JPA entity. */
@Entity
@Table(name = "analytics_rsi")
@Getter
@Setter
public class AnalyticsRSI {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long instrumentId;

    private String instrumentSymbol;

    private LocalDate tradeDate;

    private BigDecimal rsi14;

    private Instant createdAt;

    private Instant updatedAt;

}
