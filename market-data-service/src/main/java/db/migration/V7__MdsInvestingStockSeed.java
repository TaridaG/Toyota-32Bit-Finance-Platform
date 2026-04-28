package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

public class V7__MdsInvestingStockSeed extends BaseJavaMigration {

    private static final List<String> STOCKS = List.of("GARAN", "THYAO", "ASELS");

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        String product = conn.getMetaData().getDatabaseProductName();
        if (product != null && product.toLowerCase(Locale.ROOT).contains("postgres")) {
            seedPostgres(conn);
        } else {
            seedH2Standalone(conn);
        }
    }

    private void seedPostgres(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            for (String sym : STOCKS) {
                st.executeUpdate(
                        """
                                INSERT INTO mds_instrument_catalog (
                                    instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active
                                )
                                SELECT i.id,
                                       i.symbol,
                                       i.type,
                                       NULL,
                                       NULL,
                                       i.active
                                FROM instruments i
                                WHERE i.symbol = '%s'
                                  AND NOT EXISTS (SELECT 1 FROM mds_instrument_catalog c WHERE c.instrument_id = i.id)
                                """.formatted(sym)
                );
                st.executeUpdate(
                        """
                                INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                                SELECT 'INVESTING_STOCK', '%s', i.id, 0, TRUE
                                FROM instruments i
                                WHERE i.symbol = '%s'
                                  AND NOT EXISTS (
                                      SELECT 1 FROM mds_provider_instrument_mapping m
                                      WHERE m.provider = 'INVESTING_STOCK' AND m.provider_symbol = '%s'
                                  )
                                """.formatted(sym, sym, sym)
                );
            }
        }
    }

    private void seedH2Standalone(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            long id = 201L;
            for (String sym : STOCKS) {
                st.executeUpdate(
                        """
                                INSERT INTO mds_instrument_catalog (
                                    instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active
                                )
                                SELECT %d, '%s', 'STOCK', NULL, NULL, TRUE
                                WHERE NOT EXISTS (SELECT 1 FROM mds_instrument_catalog WHERE instrument_id = %d)
                                """.formatted(id, sym, id)
                );
                st.executeUpdate(
                        """
                                INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                                SELECT 'INVESTING_STOCK', '%s', %d, 0, TRUE
                                WHERE NOT EXISTS (
                                    SELECT 1 FROM mds_provider_instrument_mapping m
                                    WHERE m.provider = 'INVESTING_STOCK' AND m.provider_symbol = '%s'
                                )
                                """.formatted(sym, id, sym)
                );
                id++;
            }
        }
    }
}
