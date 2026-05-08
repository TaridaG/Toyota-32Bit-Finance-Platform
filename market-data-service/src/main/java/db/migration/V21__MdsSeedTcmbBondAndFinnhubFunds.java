package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

public class V21__MdsSeedTcmbBondAndFinnhubFunds extends BaseJavaMigration {

    private static final List<String> FINNHUB_FUNDS = List.of("VOO", "VTI", "QQQ", "IVV", "SPY");
    private static final List<String> TCMB_BONDS = List.of("TRBOND1Y", "TRBOND2Y", "TRBOND3Y", "TRBOND5Y", "TRBOND10Y");

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        String product = conn.getMetaData().getDatabaseProductName();
        if (product != null && product.toLowerCase(Locale.ROOT).contains("postgres")) {
            seedPostgres(conn);
        }
    }

    private void seedPostgres(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            for (String symbol : FINNHUB_FUNDS) {
                st.executeUpdate("""
                        INSERT INTO mds_instrument_catalog (
                            instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active
                        )
                        SELECT i.id, i.symbol, i.type, NULL, NULL, i.active
                        FROM instruments i
                        WHERE i.symbol = '%s'
                          AND NOT EXISTS (SELECT 1 FROM mds_instrument_catalog c WHERE c.instrument_id = i.id)
                        """.formatted(symbol));
                st.executeUpdate("""
                        INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                        SELECT 'FINNHUB', '%s', i.id, 0, TRUE
                        FROM instruments i
                        WHERE i.symbol = '%s'
                          AND NOT EXISTS (
                            SELECT 1 FROM mds_provider_instrument_mapping m
                            WHERE m.provider = 'FINNHUB' AND m.provider_symbol = '%s'
                          )
                        """.formatted(symbol, symbol, symbol));
            }
            for (String symbol : TCMB_BONDS) {
                st.executeUpdate("""
                        INSERT INTO mds_instrument_catalog (
                            instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active
                        )
                        SELECT i.id, i.symbol, i.type, NULL, 'TRY', i.active
                        FROM instruments i
                        WHERE i.symbol = '%s'
                          AND NOT EXISTS (SELECT 1 FROM mds_instrument_catalog c WHERE c.instrument_id = i.id)
                        """.formatted(symbol));
                st.executeUpdate("""
                        INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                        SELECT 'TCMB_BOND', '%s', i.id, 0, TRUE
                        FROM instruments i
                        WHERE i.symbol = '%s'
                          AND NOT EXISTS (
                            SELECT 1 FROM mds_provider_instrument_mapping m
                            WHERE m.provider = 'TCMB_BOND' AND m.provider_symbol = '%s'
                          )
                        """.formatted(symbol, symbol, symbol));
            }
        }
    }
}
