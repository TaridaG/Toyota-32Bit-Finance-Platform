package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Locale;

public class V5__FundTefasPlaceholderH2 extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        String product = conn.getMetaData().getDatabaseProductName();
        if (product == null || !product.toLowerCase(Locale.ROOT).contains("h2")) {
            return;
        }
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                    INSERT INTO mds_instrument_catalog (instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active)
                    SELECT 100, 'FUND_IVY', 'FUND', NULL, NULL, TRUE
                    WHERE NOT EXISTS (SELECT 1 FROM mds_instrument_catalog WHERE instrument_id = 100)
                    """);
            st.executeUpdate("""
                    INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                    SELECT 'TEFAS', 'IVY', 100, 0, TRUE
                    WHERE NOT EXISTS (
                        SELECT 1 FROM mds_provider_instrument_mapping m
                        WHERE m.provider = 'TEFAS' AND m.provider_symbol = 'IVY'
                    )
                    """);
        }
    }
}
