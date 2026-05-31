package com.company.marketdataservice.spot.infrastructure.scheduler;
import com.company.marketdataservice.bootstrap.config.TcmbBondMarketProperties;
import com.company.marketdataservice.spot.domain.MarketPriceUpdatedEvent;
import com.company.marketdataservice.catalog.application.InstrumentMappingService;
import com.company.marketdataservice.spot.infrastructure.kafka.MarketEventPublisher;
import com.company.marketdataservice.shared.provider.tcmb.TcmbBondEvdsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * `spot fiyat` verisini periyodik olarak fetch edip snapshot/Kafka'ya publish eden scheduler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "market.bond.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class BondPriceScheduler {

    private static final String SOURCE = "TCMB_BOND";

    private final TcmbBondMarketProperties bondProperties;
    private final TcmbBondEvdsClient tcmbBondEvdsClient;
    private final MarketEventPublisher publisher;
    private final InstrumentMappingService instrumentMappingService;

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    @Scheduled(fixedDelayString = "${scheduler.bond.delay-ms:300000}")
    public void pullBondPrices() {
        List<TcmbBondMarketProperties.TcmbBondSeries> tracked = bondProperties.getTracked();
        if (tracked == null || tracked.isEmpty()) {
            return;
        }
        for (TcmbBondMarketProperties.TcmbBondSeries row : tracked) {
            if (row == null || row.getSymbol() == null || row.getSymbol().isBlank()) {
                continue;
            }
            String symbol = row.getSymbol().trim().toUpperCase();
            try {
                BigDecimal price = tcmbBondEvdsClient.fetchLatestValue(row.getEvdsSeries());
                Long instrumentId = instrumentMappingService.resolveInstrument(SOURCE, symbol).orElse(null);
                publisher.publishMarketPriceUpdated(
                        MarketPriceUpdatedEvent.of(symbol, price, "MARKET", SOURCE, instrumentId)
                );
                log.info("BOND_DATA_PUBLISHED source={} symbol={} price={} instrumentId={}", SOURCE, symbol, price, instrumentId);
            } catch (Exception ex) {
                log.warn("BOND_DATA_ERROR symbol={} series={} error={}", symbol, row.getEvdsSeries(), ex.getMessage());
            }
        }
    }
}
