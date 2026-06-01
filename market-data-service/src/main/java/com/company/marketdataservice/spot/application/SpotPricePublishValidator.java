package com.company.marketdataservice.spot.application;

import com.company.marketdataservice.spot.domain.MarketPriceUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Canlı spot publish öncesi aşırı sapma ve geçersiz fiyatları filtreler. */
@Slf4j
@Component
public class SpotPricePublishValidator {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal MAX_UP_RATIO = new BigDecimal("1.6");
    private static final BigDecimal MAX_DOWN_RATIO = new BigDecimal("0.625");

    private final Map<String, BigDecimal> lastAcceptedBySymbol = new ConcurrentHashMap<>();

    /**
     * @return false ise event history/kafka'ya yazılmamalı
     */
    public boolean shouldAccept(MarketPriceUpdatedEvent event) {
        if (event == null || event.instrumentSymbol() == null || event.instrumentSymbol().isBlank()) {
            return false;
        }
        BigDecimal price = event.price();
        if (price == null || price.compareTo(ZERO) <= 0) {
            return false;
        }
        String symbol = event.instrumentSymbol().trim().toUpperCase(Locale.ROOT);
        BigDecimal previous = lastAcceptedBySymbol.get(symbol);
        if (previous == null || previous.compareTo(ZERO) <= 0) {
            lastAcceptedBySymbol.put(symbol, price);
            return true;
        }
        BigDecimal ratio = price.divide(previous, 8, RoundingMode.HALF_UP);
        if (ratio.compareTo(MAX_UP_RATIO) > 0 || ratio.compareTo(MAX_DOWN_RATIO) < 0) {
            log.warn(
                    "spot_price_outlier_rejected symbol={} price={} previous={} ratio={} eventId={}",
                    symbol,
                    price,
                    previous,
                    ratio,
                    event.eventId()
            );
            return false;
        }
        lastAcceptedBySymbol.put(symbol, price);
        return true;
    }
}
