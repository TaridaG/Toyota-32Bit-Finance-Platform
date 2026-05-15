package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

/**
 * Seeds MDS catalog + Yahoo provider mapping for Turkey USD benchmark yields ({@code TRGOVUSD*}).
 */
public class V37__MdsSeedTrGovUsdEurobondCatalog extends BaseJavaMigration {

    private static final List<String> TR_GOV_USD =
            List.of("TRGOVUSD1Y", "TRGOVUSD2Y", "TRGOVUSD3Y", "TRGOVUSD4Y", "TRGOVUSD5Y", "TRGOVUSD6Y", "TRGOVUSD8Y", "TRGOVUSD15Y");

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
            for (String symbol : TR_GOV_USD) {
                st.executeUpdate("""
                        INSERT INTO mds_instrument_catalog (
                            instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active
                        )
                        SELECT i.id, i.symbol, i.type, NULL, 'USD', i.active
                        FROM instruments i
                        WHERE i.symbol = '%s'
                          AND NOT EXISTS (SELECT 1 FROM mds_instrument_catalog c WHERE c.instrument_id = i.id)
                        """.formatted(symbol));
                st.executeUpdate("""
                        INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                        SELECT 'YAHOO', '%s', i.id, 0, TRUE
                        FROM instruments i
                        WHERE i.symbol = '%s'
                          AND NOT EXISTS (
                            SELECT 1 FROM mds_provider_instrument_mapping m
                            WHERE m.provider = 'YAHOO' AND m.provider_symbol = '%s'
                          )
                        """.formatted(symbol, symbol, symbol));
            }
        }
    }
}
