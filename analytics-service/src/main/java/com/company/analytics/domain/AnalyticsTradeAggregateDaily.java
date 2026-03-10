package com.company.analytics.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "analytics_trade_aggregate_daily")
@Getter
@Setter
public class AnalyticsTradeAggregateDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long instrumentId;

    private String instrumentSymbol;

    private LocalDate tradeDate;

    private Long tradeCount;

    private BigDecimal totalVolume;

    private BigDecimal buyVolume;

    private BigDecimal sellVolume;

    private BigDecimal avgPrice;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

}