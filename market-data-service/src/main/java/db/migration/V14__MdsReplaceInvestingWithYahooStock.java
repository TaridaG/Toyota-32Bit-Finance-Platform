package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

public class V14__MdsReplaceInvestingWithYahooStock extends BaseJavaMigration {

    private static final List<String> TURKISH_STOCKS = List.of("GARAN", "THYAO", "ASELS");
    private static final List<String> US_STOCKS = List.of("AAPL", "MSFT");

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        String product = conn.getMetaData().getDatabaseProductName();
        if (product != null && product.toLowerCase(Locale.ROOT).contains("postgres")) {
            migratePostgres(conn);
        } else {
            migrateH2(conn);
        }
    }

    private void migratePostgres(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                    UPDATE mds_provider_instrument_mapping
                    SET provider = 'YAHOO'
                    WHERE provider = 'INVESTING_STOCK'
                    """);

            for (String symbol : TURKISH_STOCKS) {
                st.executeUpdate("""
                        UPDATE mds_provider_instrument_mapping
                        SET provider_symbol = '%s.IS'
                        WHERE provider = 'YAHOO' AND provider_symbol = '%s'
                        """.formatted(symbol, symbol));
                st.executeUpdate("""
                        INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                        SELECT 'YAHOO', '%s.IS', i.id, 0, TRUE
                        FROM instruments i
                        WHERE i.symbol = '%s' AND i.type = 'STOCK'
                          AND NOT EXISTS (
                              SELECT 1 FROM mds_provider_instrument_mapping m
                              WHERE m.provider = 'YAHOO' AND m.provider_symbol = '%s.IS'
                          )
                        """.formatted(symbol, symbol, symbol));
            }

            for (String symbol : US_STOCKS) {
                st.executeUpdate("""
                        INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                        SELECT 'YAHOO', '%s', i.id, 0, TRUE
                        FROM instruments i
                        WHERE i.symbol = '%s' AND i.type = 'STOCK'
                          AND NOT EXISTS (
                              SELECT 1 FROM mds_provider_instrument_mapping m
                              WHERE m.provider = 'YAHOO' AND m.provider_symbol = '%s'
                          )
                        """.formatted(symbol, symbol, symbol));
            }
        } catch (SQLException ex) {
            if (isUndefinedTable(ex)) {
                throw new IllegalStateException(
                        "yahoo stock seed requires public.instruments; start finance-api migrations before market-data-service",
                        ex
                );
            }
            throw ex;
        }
    }

    private void migrateH2(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                    UPDATE mds_provider_instrument_mapping
                    SET provider = 'YAHOO'
                    WHERE provider = 'INVESTING_STOCK'
                    """);
            for (String symbol : TURKISH_STOCKS) {
                st.executeUpdate("""
                        UPDATE mds_provider_instrument_mapping
                        SET provider_symbol = '%s.IS'
                        WHERE provider = 'YAHOO' AND provider_symbol = '%s'
                        """.formatted(symbol, symbol));
            }
        }
    }

    private static boolean isUndefinedTable(SQLException ex) {
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
}
