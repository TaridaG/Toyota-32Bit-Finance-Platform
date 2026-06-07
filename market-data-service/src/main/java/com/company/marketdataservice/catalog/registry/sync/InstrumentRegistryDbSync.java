package com.company.marketdataservice.catalog.registry.sync;

import com.company.marketdataservice.catalog.registry.AssetKind;
import com.company.marketdataservice.catalog.registry.IngestInstrumentDef;
import com.company.marketdataservice.catalog.registry.IngestProvider;
import com.company.marketdataservice.catalog.registry.PlatformIngestRegistry;
import com.company.marketdataservice.catalog.registry.providers.FxRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/**
 * Startup'ta {@link PlatformIngestRegistry} tanımlarını paylaşılan {@code instruments} ve MDS catalog tablolarına sync eder.
 */
@Component
public class InstrumentRegistryDbSync {
    private static final Logger log = LoggerFactory.getLogger(InstrumentRegistryDbSync.class);

    private static final String UPSERT_INSTRUMENT = """
            INSERT INTO instruments (symbol, name, type, exchange, active)
            VALUES (?, ?, ?, ?, TRUE)
            ON CONFLICT (symbol) DO UPDATE SET
              name = EXCLUDED.name,
              type = EXCLUDED.type,
              exchange = EXCLUDED.exchange,
              active = TRUE
            RETURNING id
            """;

    private static final String UPSERT_CATALOG = """
            INSERT INTO mds_instrument_catalog (
                instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active
            )
            VALUES (?, ?, ?, ?, ?, TRUE)
            ON CONFLICT (instrument_id) DO UPDATE SET
              canonical_symbol = EXCLUDED.canonical_symbol,
              asset_class = EXCLUDED.asset_class,
              base_currency = EXCLUDED.base_currency,
              quote_currency = EXCLUDED.quote_currency,
              active = TRUE
            """;

    private static final String UPSERT_MAPPING = """
            INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
            VALUES (?, ?, ?, ?, TRUE)
            ON CONFLICT (provider, provider_symbol) DO UPDATE SET
              instrument_id = EXCLUDED.instrument_id,
              priority = EXCLUDED.priority,
              active = TRUE
            """;
    private static final String UPSERT_INGEST_CONFIG = """
            INSERT INTO mds_ingest_config (instrument_id, segment, enabled)
            VALUES (?, ?, TRUE)
            ON CONFLICT (instrument_id, segment) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;

    public InstrumentRegistryDbSync(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.meterRegistry = meterRegistry;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    public void onApplicationReady() {
        if (!financeInstrumentsAvailable()) {
            log.info("REGISTRY_DB_SYNC_SKIP reason=finance_instruments_unavailable");
            return;
        }
        syncAll();
    }

    @Transactional
    public void syncAll() {
        List<IngestInstrumentDef> defs = PlatformIngestRegistry.all();
        int success = 0;
        int failed = 0;
        for (IngestInstrumentDef def : defs) {
            try {
                syncOne(def);
                success++;
            } catch (Exception ex) {
                failed++;
                meterRegistry.counter(
                        "registry_sync_failure_total",
                        Tags.of("service", "market-data-service", "symbol", def.symbol())
                ).increment();
                log.warn("REGISTRY_DB_SYNC_FAILED symbol={} reason={}", def.symbol(), ex.getMessage());
            }
        }
        log.info("REGISTRY_DB_SYNC_COMPLETE success={} failed={} total={}", success, failed, defs.size());
    }

    private void syncOne(IngestInstrumentDef def) {
        Long instrumentId = upsertInstrument(def);
        upsertCatalog(def, instrumentId);
        upsertMappings(def, instrumentId);
        upsertIngestConfig(def, instrumentId);
    }

    private Long upsertInstrument(IngestInstrumentDef def) {
        return jdbcTemplate.queryForObject(
                UPSERT_INSTRUMENT,
                Long.class,
                def.symbol(),
                def.displayName(),
                def.kind().name(),
                def.exchange()
        );
    }

    private void upsertCatalog(IngestInstrumentDef def, Long instrumentId) {
        String baseCurrency = resolveBaseCurrency(def);
        String quoteCurrency = def.quoteCurrency().name();
        jdbcTemplate.update(
                UPSERT_CATALOG,
                instrumentId,
                def.symbol(),
                def.kind().name(),
                baseCurrency,
                quoteCurrency
        );
    }

    private void upsertMappings(IngestInstrumentDef def, Long instrumentId) {
        if (def.kind() == AssetKind.CRYPTO) {
            upsertMapping("BINANCE", def.symbol(), instrumentId, 0);
            upsertMapping("COINGECKO", def.symbol(), instrumentId, 10);
            return;
        }
        IngestProvider provider = def.provider();
        if (provider == IngestProvider.COMPOSITE) {
            return;
        }
        String providerName = provider.name();
        String providerSymbol = def.resolvedProviderSymbol();
        upsertMapping(providerName, providerSymbol, instrumentId, 0);
    }

    private void upsertMapping(String provider, String providerSymbol, Long instrumentId, int priority) {
        jdbcTemplate.update(UPSERT_MAPPING, provider, providerSymbol, instrumentId, priority);
    }

    private void upsertIngestConfig(IngestInstrumentDef def, Long instrumentId) {
        String segment = resolveIngestSegment(def);
        if (segment == null) {
            return;
        }
        jdbcTemplate.update(UPSERT_INGEST_CONFIG, instrumentId, segment);
    }

    private static String resolveIngestSegment(IngestInstrumentDef def) {
        if (def.kind() == AssetKind.CRYPTO) {
            return "CRYPTO";
        }
        if (def.kind() != AssetKind.STOCK) {
            return null;
        }
        String exchange = def.exchange();
        if ("BIST".equalsIgnoreCase(exchange)) {
            return "BIST";
        }
        if ("NASDAQ".equalsIgnoreCase(exchange)
                || "FINNHUB".equalsIgnoreCase(exchange)
                || "YAHOO".equalsIgnoreCase(exchange)) {
            return "NASDAQ";
        }
        return null;
    }

    private static String resolveBaseCurrency(IngestInstrumentDef def) {
        if (def.kind() == AssetKind.CRYPTO && def.symbol().endsWith("USDT")) {
            return def.symbol().substring(0, def.symbol().length() - 4);
        }
        if (def.kind() == AssetKind.FX) {
            return FxRegistry.baseCurrencyFor(def.symbol());
        }
        if (def.kind() == AssetKind.EUROBOND) {
            return "USD";
        }
        if (def.kind() == AssetKind.BOND) {
            return null;
        }
        if (def.quoteCurrency() == com.company.marketdataservice.catalog.registry.QuoteCurrency.TRY) {
            return null;
        }
        if (def.quoteCurrency() == com.company.marketdataservice.catalog.registry.QuoteCurrency.USD) {
            return def.symbol();
        }
        return null;
    }

    private boolean financeInstrumentsAvailable() {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase(Locale.ROOT).contains("postgres")) {
                return false;
            }
            try (var st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT to_regclass('public.instruments') IS NOT NULL AS ok")) {
                return rs.next() && rs.getBoolean("ok");
            }
        } catch (SQLException ex) {
            return false;
        }
    }
}
