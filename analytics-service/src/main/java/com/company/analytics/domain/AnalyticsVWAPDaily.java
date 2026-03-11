package com.company.analytics.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "analytics_vwap_daily")
@Getter
@Setter
public class AnalyticsVWAPDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long instrumentId;

    private String instrumentSymbol;

    private LocalDate tradeDate;

    private BigDecimal totalVolume;

    private BigDecimal totalPriceVolume;

    private BigDecimal vwap;

    private Instant createdAt;

    private Instant updatedAt;

    public static AnalyticsVWAPDaily create(
            Long instrumentId,
            String symbol,
            LocalDate date
    ){

        AnalyticsVWAPDaily v = new AnalyticsVWAPDaily();

        v.setInstrumentId(instrumentId);
        v.setInstrumentSymbol(symbol);
        v.setTradeDate(date);

        v.setTotalVolume(BigDecimal.ZERO);
        v.setTotalPriceVolume(BigDecimal.ZERO);
        v.setVwap(BigDecimal.ZERO);

        v.setCreatedAt(Instant.now());
        v.setUpdatedAt(Instant.now());

        return v;

    }

    public void applyTrade(BigDecimal price, BigDecimal quantity){

        totalVolume = totalVolume.add(quantity);

        totalPriceVolume =
                totalPriceVolume.add(
                        price.multiply(quantity)
                );

        vwap = totalPriceVolume.divide(
                totalVolume,
                8,
                RoundingMode.HALF_UP
        );

        updatedAt = Instant.now();

    }

}