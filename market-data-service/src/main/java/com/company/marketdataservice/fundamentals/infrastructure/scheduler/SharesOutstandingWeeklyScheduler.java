package com.company.marketdataservice.fundamentals.infrastructure.scheduler;
import com.company.marketdataservice.bootstrap.config.MarketDataProperties;
import com.company.marketdataservice.bootstrap.config.FinnhubProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.company.marketdataservice.fundamentals.infrastructure.persistence.InstrumentSharesOutstandingEntry;
import com.company.marketdataservice.fundamentals.infrastructure.persistence.InstrumentSharesOutstandingRepository;
import com.company.marketdataservice.catalog.infrastructure.persistence.InstrumentCatalogEntry;
import com.company.marketdataservice.catalog.infrastructure.persistence.InstrumentCatalogRepository;
import com.company.marketdataservice.spot.infrastructure.provider.finnhub.FinnhubClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * `temel veri (fundamentals)` verisini periyodik olarak fetch edip snapshot/Kafka'ya publish eden scheduler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "market.stock.shares-verification.enabled", havingValue = "true", matchIfMissing = true)
public class SharesOutstandingWeeklyScheduler {
    private final MarketDataProperties marketDataProperties;
    private final FinnhubProperties finnhubProperties;
    private final InstrumentCatalogRepository instrumentCatalogRepository;
    private final InstrumentSharesOutstandingRepository sharesRepository;
    private final FinnhubClient finnhubClient;

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    @Scheduled(cron = "${market.stock.shares-verification.cron:0 0 3 * * MON}", zone = "UTC")
    public void verifySharesOutstandingWeekly() {
        refreshSharesOutstanding();
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    @EventListener(ApplicationReadyEvent.class)
    public void warmupSharesOutstandingOnStartup() {
        refreshSharesOutstanding();
    }

    private void refreshSharesOutstanding() {
        List<String> stocks = marketDataProperties.getTrackedStocks();
        if (stocks == null || stocks.isEmpty()) {
            return;
        }
        for (String symbol : stocks) {
            if (symbol == null || symbol.isBlank()) {
                continue;
            }
            String canonical = symbol.trim().toUpperCase(Locale.ROOT);
            InstrumentCatalogEntry instrument = instrumentCatalogRepository.findByCanonicalSymbolAndActiveTrue(canonical).orElse(null);
            if (instrument == null || !"STOCK".equalsIgnoreCase(instrument.getAssetClass())) {
                continue;
            }
            BigDecimal shares = fetchSharesWithFallback(canonical);
            if (shares == null || shares.signum() <= 0) {
                continue;
            }
            InstrumentSharesOutstandingEntry row = sharesRepository.findById(instrument.getInstrumentId())
                    .orElseGet(InstrumentSharesOutstandingEntry::new);
            row.setInstrumentId(instrument.getInstrumentId());
            row.setCanonicalSymbol(canonical);
            row.setSharesOutstanding(shares);
            row.setSource("FINNHUB");
            row.setProviderSymbol(canonical);
            row.setVerifiedAt(Instant.now());
            sharesRepository.save(row);
        }
    }

    private BigDecimal fetchSharesWithFallback(String canonical) {
        if (!finnhubProperties.isEnabled()) {
            return null;
        }
        List<String> candidates = canonical.contains(".")
                ? List.of(canonical)
                : List.of(canonical, canonical + ".IS");
        for (String candidate : candidates) {
            try {
                JsonNode metric = finnhubClient.fetchBasicFinancials(candidate).path("metric");
                JsonNode node = metric.path("shareOutstanding");
                if (!node.isMissingNode() && !node.isNull()) {
                    return node.decimalValue();
                }
            } catch (Exception ex) {
                log.debug("SHARES_VERIFICATION_FAILED symbol={} candidate={} reason={}", canonical, candidate, ex.getMessage());
            }
        }
        return null;
    }

}

