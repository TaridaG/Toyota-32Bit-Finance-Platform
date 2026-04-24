package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Locale;

public class V3__MdsFxTcmbCatalogSeed extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        String product = conn.getMetaData().getDatabaseProductName();
        if (product != null && product.toLowerCase(Locale.ROOT).contains("postgres")) {
            seedPostgres(conn);
        } else {
            seedH2(conn);
        }
    }

    private void seedPostgres(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                    INSERT INTO mds_instrument_catalog (
                        instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active
                    )
                    SELECT i.id,
                           i.symbol,
                           i.type,
                           CASE
                               WHEN i.symbol LIKE '%TRY' AND LENGTH(i.symbol) > 3
                                   THEN SUBSTRING(i.symbol, 1, LENGTH(i.symbol) - 3)
                               ELSE NULL
                           END,
                           CASE
                               WHEN i.symbol LIKE '%TRY' THEN 'TRY'
                               ELSE NULL
                           END,
                           i.active
                    FROM instruments i
                    WHERE i.symbol IN ('USDTRY', 'EURTRY')
                      AND NOT EXISTS (SELECT 1 FROM mds_instrument_catalog c WHERE c.instrument_id = i.id)
                    """);
            st.executeUpdate("""
                    INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                    SELECT 'TCMB', i.symbol, i.id, 0, TRUE
                    FROM instruments i
                    WHERE i.symbol IN ('USDTRY', 'EURTRY')
                      AND NOT EXISTS (
                          SELECT 1 FROM mds_provider_instrument_mapping m
                          WHERE m.provider = 'TCMB' AND m.provider_symbol = i.symbol
                      )
                    """);
        }
    }

    private void seedH2(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                    INSERT INTO mds_instrument_catalog (
                        instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active
                    )
                    SELECT 6, 'USDTRY', 'FX', 'USD', 'TRY', TRUE
                    WHERE NOT EXISTS (SELECT 1 FROM mds_instrument_catalog WHERE instrument_id = 6)
                    """);
            st.executeUpdate("""
                    INSERT INTO mds_instrument_catalog (
                        instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active
                    )
                    SELECT 7, 'EURTRY', 'FX', 'EUR', 'TRY', TRUE
                    WHERE NOT EXISTS (SELECT 1 FROM mds_instrument_catalog WHERE instrument_id = 7)
                    """);
            st.executeUpdate("""
                    INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                    SELECT 'TCMB', 'USDTRY', 6, 0, TRUE
                    WHERE NOT EXISTS (
                        SELECT 1 FROM mds_provider_instrument_mapping m
                        WHERE m.provider = 'TCMB' AND m.provider_symbol = 'USDTRY'
                    )
                    """);
            st.executeUpdate("""
                    INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                    SELECT 'TCMB', 'EURTRY', 7, 0, TRUE
                    WHERE NOT EXISTS (
                        SELECT 1 FROM mds_provider_instrument_mapping m
                        WHERE m.provider = 'TCMB' AND m.provider_symbol = 'EURTRY'
                    )
                    """);
        }
    }
}
