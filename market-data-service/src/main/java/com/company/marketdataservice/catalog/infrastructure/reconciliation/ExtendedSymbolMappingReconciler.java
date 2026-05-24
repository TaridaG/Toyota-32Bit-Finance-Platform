package com.company.marketdataservice.catalog.infrastructure.reconciliation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

/**
 * `enstrüman kataloğu` infrastructure katmanı adaptörü.
 */
@Component
@Slf4j
public class ExtendedSymbolMappingReconciler {

    private static final List<String> FINNHUB_FUNDS = List.of("VOO", "VTI", "QQQ", "IVV", "SPY");
    private static final List<String> TCMB_BONDS = List.of("TRBOND1Y", "TRBOND2Y", "TRBOND3Y", "TRBOND5Y", "TRBOND10Y");
    /** Turkey USD sovereign benchmark yields (Yahoo chart tickers GTUSDTR*Y:GOV). */
    private static final List<String> TR_GOV_USD_EUROBONDS =
            List.of("TRGOVUSD1Y", "TRGOVUSD2Y", "TRGOVUSD3Y", "TRGOVUSD4Y", "TRGOVUSD5Y", "TRGOVUSD6Y", "TRGOVUSD8Y", "TRGOVUSD15Y");
    private static final List<String> TCMB_METALS = List.of("XAUTRY", "XAGTRY", "XPTTRY", "XPDTRY", "XCUTRY");
    private static final List<String> YAHOO_METAL_FUTURES = List.of("GC=F", "SI=F", "HG=F", "PA=F", "PL=F");

    private final JdbcTemplate jdbcTemplate;

    public ExtendedSymbolMappingReconciler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Katalog mapping reconciliation uygular.
         */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reconcile() {
        if (!financeInstrumentsAvailable()) {
            log.debug("EXTENDED_SYMBOL_MAPPING_SKIP reason=finance_instruments_unavailable");
            return;
        }
        int applied = 0;
        for (String symbol : FINNHUB_FUNDS) {
            applied += upsertCatalog(symbol, null);
            applied += upsertProviderMapping(symbol, "FINNHUB");
        }
        for (String symbol : TCMB_BONDS) {
            applied += upsertCatalog(symbol, "TRY");
            applied += upsertProviderMapping(symbol, "TCMB_BOND");
        }
        for (String symbol : TR_GOV_USD_EUROBONDS) {
            applied += upsertCatalog(symbol, "USD");
            applied += upsertProviderMapping(symbol, "YAHOO");
        }
        for (String symbol : TCMB_METALS) {
            applied += upsertCatalog(symbol, "TRY");
            applied += upsertProviderMapping(symbol, "TCMB");
        }
        for (String symbol : YAHOO_METAL_FUTURES) {
            applied += upsertCatalog(symbol, "USD");
            applied += upsertProviderMapping(symbol, "YAHOO");
        }
        log.info("EXTENDED_SYMBOL_MAPPING_RECONCILED changes={}", applied);
    }

    private boolean financeInstrumentsAvailable() {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase(Locale.ROOT).contains("postgres")) {
                return false;
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT to_regclass('public.instruments') IS NOT NULL AS ok")) {
                return rs.next() && rs.getBoolean("ok");
            }
        } catch (SQLException ex) {
            return false;
        }
    }

    private int upsertCatalog(String symbol, String quoteCurrency) {
        String sql = """
                INSERT INTO mds_instrument_catalog (
                    instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active
                )
                SELECT i.id, i.symbol, i.type, NULL, ?, i.active
                FROM instruments i
                WHERE i.symbol = ?
                  AND NOT EXISTS (SELECT 1 FROM mds_instrument_catalog c WHERE c.instrument_id = i.id)
                """;
        return jdbcTemplate.update(sql, quoteCurrency, symbol);
    }

    private int upsertProviderMapping(String symbol, String provider) {
        String sql = """
                INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                SELECT ?, ?, i.id, 0, TRUE
                FROM instruments i
                WHERE i.symbol = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM mds_provider_instrument_mapping m
                      WHERE m.provider = ? AND m.provider_symbol = ?
                  )
                """;
        return jdbcTemplate.update(sql, provider, symbol, symbol, provider, symbol);
    }
}
