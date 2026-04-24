package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Locale;

public class V6__MdsTefasFundPostgresCatalogAndMapping extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        String product = conn.getMetaData().getDatabaseProductName();
        if (product == null || !product.toLowerCase(Locale.ROOT).contains("postgres")) {
            return;
        }
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
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
                    WHERE i.symbol = 'FUND_IVY'
                      AND NOT EXISTS (SELECT 1 FROM mds_instrument_catalog c WHERE c.instrument_id = i.id)
                    """);
            st.executeUpdate("""
                    INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                    SELECT 'TEFAS', 'IVY', i.id, 0, TRUE
                    FROM instruments i
                    WHERE i.symbol = 'FUND_IVY'
                      AND NOT EXISTS (
                          SELECT 1 FROM mds_provider_instrument_mapping m
                          WHERE m.provider = 'TEFAS' AND m.provider_symbol = 'IVY'
                      )
                    """);
        }
    }
}
