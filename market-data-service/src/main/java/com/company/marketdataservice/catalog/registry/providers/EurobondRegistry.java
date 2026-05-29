package com.company.marketdataservice.catalog.registry.providers;

import com.company.marketdataservice.catalog.registry.AssetKind;
import com.company.marketdataservice.catalog.registry.IngestInstrumentDef;
import com.company.marketdataservice.catalog.registry.IngestProvider;
import com.company.marketdataservice.catalog.registry.QuoteCurrency;

import java.util.List;

/**
 * Turkey USD sovereign benchmark yields (Yahoo chart tickers).
 */
public final class EurobondRegistry {

    private EurobondRegistry() {}

    private record EurobondRow(String canonical, String yahooChartSymbol) {}

    private static final List<EurobondRow> ROWS = List.of(
            new EurobondRow("TRGOVUSD1Y", "GTUSDTR1Y:GOV"),
            new EurobondRow("TRGOVUSD2Y", "GTUSDTR2Y:GOV"),
            new EurobondRow("TRGOVUSD3Y", "GTUSDTR3Y:GOV"),
            new EurobondRow("TRGOVUSD4Y", "GTUSDTR4Y:GOV"),
            new EurobondRow("TRGOVUSD5Y", "GTUSDTR5Y:GOV"),
            new EurobondRow("TRGOVUSD6Y", "GTUSDTR6Y:GOV"),
            new EurobondRow("TRGOVUSD8Y", "GTUSDTR8Y:GOV"),
            new EurobondRow("TRGOVUSD15Y", "GTUSDTR15Y:GOV"));

    public static List<IngestInstrumentDef> all() {
        return ROWS.stream().map(EurobondRegistry::def).toList();
    }

    public static List<EurobondIngestRow> ingestRows() {
        return ROWS.stream()
                .map(row -> new EurobondIngestRow(row.canonical(), row.yahooChartSymbol()))
                .toList();
    }

    public record EurobondIngestRow(String canonical, String yahooChartSymbol) {}

    private static IngestInstrumentDef def(EurobondRow row) {
        return new IngestInstrumentDef(
                row.canonical(),
                "TR Gov USD " + row.canonical(),
                AssetKind.EUROBOND,
                "YAHOO",
                QuoteCurrency.USD,
                IngestProvider.YAHOO,
                row.yahooChartSymbol(),
                null,
                null,
                row.yahooChartSymbol()
        );
    }
}
