package com.company.marketdataservice.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class CatalogFinanceAlignmentMetrics implements MeterBinder {

    private static final Pattern SAFE_REL = Pattern.compile("^[a-z][a-z0-9_]*$");

    private final DataSource dataSource;

    public CatalogFinanceAlignmentMetrics(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("market_data_catalog_instruments_unmatched_to_finance_total", this,
                        CatalogFinanceAlignmentMetrics::countUnmatchedSafe)
                .tags("service", "market-data-service")
                .register(registry);
    }

    private double countUnmatchedSafe() {
        try (Connection conn = dataSource.getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase(Locale.ROOT).contains("postgres")) {
                return 0.0;
            }
            if (!relationExists(conn, "instruments") || !relationExists(conn, "mds_instrument_catalog")) {
                return 0.0;
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         """
                                 SELECT COUNT(*) AS n
                                 FROM mds_instrument_catalog c
                                 WHERE NOT EXISTS (SELECT 1 FROM instruments i WHERE i.id = c.instrument_id)
                                 """
                 )) {
                return rs.next() ? rs.getDouble("n") : 0.0;
            }
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private static boolean relationExists(Connection conn, String name) throws SQLException {
        if (!SAFE_REL.matcher(name).matches()) {
            return false;
        }
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT to_regclass('public." + name + "') IS NOT NULL AS ok"
             )) {
            return rs.next() && rs.getBoolean("ok");
        }
    }
}
