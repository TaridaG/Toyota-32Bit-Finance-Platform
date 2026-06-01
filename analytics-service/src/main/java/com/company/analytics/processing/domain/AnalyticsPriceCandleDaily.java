package com.company.analytics.processing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Instrument için günlük OHLCV mum verisini saklayan JPA entity. */
@Entity
@Table(
        name = "analytics_price_candle_daily",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_analytics_price_candle_daily", columnNames = {
                        "instrument_id", "candle_date"
                })
        }
)
@Getter
@Setter
public class AnalyticsPriceCandleDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Column(name = "instrument_symbol", nullable = false, length = 32)
    private String instrumentSymbol;

    @Column(name = "candle_date", nullable = false)
    private LocalDate candleDate;

    @Column(name = "open_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal openPrice;

    @Column(name = "high_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal highPrice;

    @Column(name = "low_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal closePrice;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal volume;

    @Column(name = "trade_count", nullable = false)
    private Long tradeCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Tek bir trade ile başlatılmış yeni bir günlük candle oluşturur.
     *
     * @param instrumentId      Instrument kimliği
     * @param instrumentSymbol  Instrument sembolü
     * @param candleDate          Mum tarihi
     * @param firstPrice          İlk trade fiyatı
     * @param firstQuantity       İlk trade miktarı
     * @return Yeni oluşturulmuş günlük candle instance'ı
     */
    public static AnalyticsPriceCandleDaily create(
            Long instrumentId,
            String instrumentSymbol,
            LocalDate candleDate,
            BigDecimal firstPrice,
            BigDecimal firstQuantity
    ) {
        AnalyticsPriceCandleDaily candle = new AnalyticsPriceCandleDaily();
        Instant now = Instant.now();

        candle.setInstrumentId(instrumentId);
        candle.setInstrumentSymbol(instrumentSymbol);
        candle.setCandleDate(candleDate);

        candle.setOpenPrice(firstPrice);
        candle.setHighPrice(firstPrice);
        candle.setLowPrice(firstPrice);
        candle.setClosePrice(firstPrice);

        candle.setVolume(firstQuantity);
        candle.setTradeCount(1L);

        candle.setCreatedAt(now);
        candle.setUpdatedAt(now);

        return candle;
    }

    /**
     * Mevcut günlük mumu yeni bir trade ile günceller; OHLC, volume ve trade count alanlarını yeniler.
     *
     * @param price    Trade fiyatı
     * @param quantity Trade miktarı
     */
    public void applyTrade(BigDecimal price, BigDecimal quantity) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (highPrice == null || highPrice.compareTo(BigDecimal.ZERO) <= 0) {
            highPrice = price;
        } else if (price.compareTo(highPrice) > 0) {
            highPrice = price;
        }
        if (lowPrice == null || lowPrice.compareTo(BigDecimal.ZERO) <= 0) {
            lowPrice = price;
        } else if (price.compareTo(lowPrice) < 0) {
            lowPrice = price;
        }

        closePrice = price;
        volume = volume.add(quantity);
        tradeCount = tradeCount + 1;
        updatedAt = Instant.now();
    }
}
