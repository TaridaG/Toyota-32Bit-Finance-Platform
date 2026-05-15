package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

/**
 * Seeds {@code mds_instrument_catalog} + {@code mds_provider_instrument_mapping} for curated TEFAS YAT funds
 * (para piyasası + altın örnekleri). Postgres uses {@code instruments} from finance-api; H2 uses fixed ids.
 */
public class V27__MdsSeedTefasTurkFundCatalog extends BaseJavaMigration {

    private record Seed(String tefasCode, String canonicalSymbol, long h2InstrumentId) {}

    private static final List<Seed> SEEDS = List.of(
            new Seed("TI2", "FUND_TI2", 101),
            new Seed("AFT", "FUND_AFT", 102),
            new Seed("AFA", "FUND_AFA", 103),
            new Seed("AFO", "FUND_AFO", 104),
            new Seed("GTA", "FUND_GTA", 105)
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        String product = conn.getMetaData().getDatabaseProductName();
        if (product == null) {
            return;
        }
        String p = product.toLowerCase(Locale.ROOT);
        if (p.contains("h2")) {
            migrateH2(conn);
        } else if (p.contains("postgres")) {
            migratePostgres(conn);
        }
    }

    private static void migrateH2(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            for (Seed s : SEEDS) {
                st.executeUpdate(
                        """
                                INSERT INTO mds_instrument_catalog (
                                    instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active
                                )
                                SELECT %d, '%s', 'FUND', NULL, NULL, TRUE
                                WHERE NOT EXISTS (SELECT 1 FROM mds_instrument_catalog WHERE instrument_id = %d)
                                """
                                .formatted(s.h2InstrumentId(), s.canonicalSymbol(), s.h2InstrumentId())
                );
                st.executeUpdate(
                        """
                                INSERT INTO mds_provider_instrument_mapping (
                                    provider, provider_symbol, instrument_id, priority, active
                                )
                                SELECT 'TEFAS', '%s', %d, 0, TRUE
                                WHERE NOT EXISTS (
                                    SELECT 1 FROM mds_provider_instrument_mapping m
                                    WHERE m.provider = 'TEFAS' AND m.provider_symbol = '%s'
                                )
                                """
                                .formatted(s.tefasCode(), s.h2InstrumentId(), s.tefasCode())
                );
            }
        }
    }

    private static void migratePostgres(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            for (Seed s : SEEDS) {
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
                                .formatted(s.canonicalSymbol())
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
                                .formatted(s.tefasCode(), s.canonicalSymbol(), s.tefasCode())
                );
            }
        }
    }
}
