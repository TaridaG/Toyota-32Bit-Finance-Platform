package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

public class V2__BootstrapMdsCatalogAndMapping extends BaseJavaMigration {

    private static final List<String> TRACKED = List.of(
            "BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT"
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        String product = conn.getMetaData().getDatabaseProductName();
        if (product != null && product.toLowerCase(Locale.ROOT).contains("postgres")) {
            seedFromFinanceInstruments(conn);
        } else {
            seedStandaloneForTests(conn);
        }
    }

    private void seedFromFinanceInstruments(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                    INSERT INTO mds_instrument_catalog (
                        instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active
                    )
                    SELECT i.id,
                           i.symbol,
                           i.type,
                           CASE WHEN i.symbol LIKE '%USDT' THEN SUBSTRING(i.symbol, 1, LENGTH(i.symbol) - 4) ELSE NULL END,
                           CASE WHEN i.symbol LIKE '%USDT' THEN 'USDT' ELSE NULL END,
                           i.active
                    FROM instruments i
                    WHERE i.symbol IN ('BTCUSDT','ETHUSDT','BNBUSDT','SOLUSDT','XRPUSDT')
                      AND NOT EXISTS (
                          SELECT 1 FROM mds_instrument_catalog c WHERE c.instrument_id = i.id
                      )
                    """);
            st.executeUpdate("""
                    INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                    SELECT 'BINANCE', i.symbol, i.id, 0, TRUE
                    FROM instruments i
                    WHERE i.symbol IN ('BTCUSDT','ETHUSDT','BNBUSDT','SOLUSDT','XRPUSDT')
                      AND NOT EXISTS (
                          SELECT 1 FROM mds_provider_instrument_mapping m
                          WHERE m.provider = 'BINANCE' AND m.provider_symbol = i.symbol
                      )
                    """);
            st.executeUpdate("""
                    INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                    SELECT 'COINGECKO', i.symbol, i.id, 10, TRUE
                    FROM instruments i
                    WHERE i.symbol IN ('BTCUSDT','ETHUSDT','BNBUSDT','SOLUSDT','XRPUSDT')
                      AND NOT EXISTS (
                          SELECT 1 FROM mds_provider_instrument_mapping m
                          WHERE m.provider = 'COINGECKO' AND m.provider_symbol = i.symbol
                      )
                    """);
        } catch (SQLException ex) {
            if (isPostgresUndefinedTable(ex)) {
                throw new IllegalStateException(
                        "mds seed requires public.instruments; start finance-api migrations before market-data-service",
                        ex
                );
            }
            throw ex;
        }
    }

    private static boolean isPostgresUndefinedTable(SQLException ex) {
        if (ex == null) {
            return false;
        }
        String state = ex.getSQLState();
        if ("42P01".equals(state)) {
            return true;
        }
        String msg = ex.getMessage();
        return msg != null && msg.toLowerCase(Locale.ROOT).contains("instruments")
                && msg.toLowerCase(Locale.ROOT).contains("does not exist");
    }

    private void seedStandaloneForTests(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            long id = 1L;
            for (String sym : TRACKED) {
                String base = sym.endsWith("USDT") ? sym.substring(0, sym.length() - 4) : sym;
                st.executeUpdate(String.format(Locale.ROOT, """
                        INSERT INTO mds_instrument_catalog (
                            instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active
                        )
                        SELECT %1$d, '%2$s', 'CRYPTO', '%3$s', 'USDT', TRUE
                        WHERE NOT EXISTS (SELECT 1 FROM mds_instrument_catalog WHERE instrument_id = %1$d)
                        """, id, sym, base));
                st.executeUpdate(String.format(Locale.ROOT, """
                        INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                        SELECT 'BINANCE', '%1$s', %2$d, 0, TRUE
                        WHERE NOT EXISTS (
                            SELECT 1 FROM mds_provider_instrument_mapping m
                            WHERE m.provider = 'BINANCE' AND m.provider_symbol = '%1$s'
                        )
                        """, sym, id));
                st.executeUpdate(String.format(Locale.ROOT, """
                        INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                        SELECT 'COINGECKO', '%1$s', %2$d, 10, TRUE
                        WHERE NOT EXISTS (
                            SELECT 1 FROM mds_provider_instrument_mapping m
                            WHERE m.provider = 'COINGECKO' AND m.provider_symbol = '%1$s'
                        )
                        """, sym, id));
                id++;
            }
        }
    }
}
