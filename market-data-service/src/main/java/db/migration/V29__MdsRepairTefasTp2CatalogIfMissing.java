package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Locale;

/**
 * If {@link V28__MdsSeedTefasTp2FundCatalog} ran before finance-api {@code V33} created {@code FUND_TP2},
 * catalog/mapping stayed empty. This migration repeats the same idempotent inserts.
 */
public class V29__MdsRepairTefasTp2CatalogIfMissing extends BaseJavaMigration {

    private static final String TEFAS_CODE = "TP2";
    private static final String CANONICAL = "FUND_TP2";

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        String product = conn.getMetaData().getDatabaseProductName();
        if (product == null) {
            return;
        }
        String p = product.toLowerCase(Locale.ROOT);
        if (p.contains("postgres")) {
            migratePostgres(conn);
        }
    }

    private static void migratePostgres(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(
                    """
                            INSERT INTO mds_instrument_catalog (
                                instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active
                            )
                            SELECT i.id,
                                   i.symbol,
                                   'FUND',
                                   NULL,
                                   NULL,
                                   i.active
                            FROM instruments i
                            WHERE i.symbol = '%s'
                              AND NOT EXISTS (SELECT 1 FROM mds_instrument_catalog c WHERE c.instrument_id = i.id)
                            """
                            .formatted(CANONICAL)
            );
            st.executeUpdate(
                    """
                            INSERT INTO mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
                            SELECT 'TEFAS', '%s', i.id, 0, TRUE
                            FROM instruments i
                            WHERE i.symbol = '%s'
                              AND NOT EXISTS (
                                  SELECT 1 FROM mds_provider_instrument_mapping m
                                  WHERE m.provider = 'TEFAS' AND m.provider_symbol = '%s'
                              )
                            """
                            .formatted(TEFAS_CODE, CANONICAL, TEFAS_CODE)
            );
        }
    }
}
