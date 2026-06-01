package com.company.analytics.processing.domain;

import com.company.analytics.processing.domain.enums.CandleInterval;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Instrument için belirli bir zaman diliminde (candle interval) OHLCV mum verisini saklayan JPA entity. */
@Entity
@Table(
        name = "price_candles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_price_candle_bucket",
                        columnNames = {"instrument_id", "candle_interval", "open_time"}
                )
        }
)
@Getter
@Setter
public class AnalyticsPriceCandle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Column(name = "instrument_symbol", nullable = false, length = 32)
    private String instrumentSymbol;

    // DEGISTIR: "interval" yerine "candleInterval" (keyword riskini azalt)
    @Enumerated(EnumType.STRING)
    @Column(name = "candle_interval", nullable = false, length = 32)
    private CandleInterval candleInterval;

    @Column(name = "open_time", nullable = false)
    private Instant openTime;

    @Column(name = "open_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal openPrice;

    @Column(name = "high_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal highPrice;

    @Column(name = "low_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal closePrice;

    @Column(name = "volume", nullable = false, precision = 19, scale = 6)
    private BigDecimal volume;

    @Column(name = "trade_count", nullable = false)
    private Long tradeCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Tek bir trade ile başlatılmış yeni bir price candle oluşturur.
     *
     * @param instrumentId   Instrument kimliği
     * @param symbol         Instrument sembolü
     * @param candleInterval Mum zaman dilimi
     * @param openTime       Mum açılış zamanı
     * @param price          İlk trade fiyatı
     * @param quantity       İlk trade miktarı
     * @return Yeni oluşturulmuş candle instance'ı
     */
    public static AnalyticsPriceCandle create(
            Long instrumentId,
            String symbol,
            CandleInterval candleInterval,
            Instant openTime,
            BigDecimal price,
            BigDecimal quantity
    ) {
        AnalyticsPriceCandle candle = new AnalyticsPriceCandle();
        Instant now = Instant.now();

        candle.instrumentId = instrumentId;
        candle.instrumentSymbol = symbol;
        candle.candleInterval = candleInterval;
        candle.openTime = openTime;

        candle.openPrice = price;
        candle.highPrice = price;
        candle.lowPrice = price;
        candle.closePrice = price;
        candle.volume = quantity;
        candle.tradeCount = 1L;
        candle.createdAt = now;
        candle.updatedAt = now;

        return candle;
    }

    /**
     * Mevcut mumu yeni bir trade ile günceller; OHLC, volume ve trade count alanlarını yeniler.
     *
     * @param price    Trade fiyatı
     * @param quantity Trade miktarı
     */
    public void applyTrade(BigDecimal price, BigDecimal quantity) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        this.closePrice = price;

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

        this.volume = this.volume.add(quantity);
        this.tradeCount = this.tradeCount + 1;
        this.updatedAt = Instant.now();
    }
}
