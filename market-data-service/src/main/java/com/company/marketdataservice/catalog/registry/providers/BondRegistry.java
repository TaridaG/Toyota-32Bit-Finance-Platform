package com.company.marketdataservice.catalog.registry.providers;

import com.company.marketdataservice.catalog.registry.AssetKind;
import com.company.marketdataservice.catalog.registry.IngestInstrumentDef;
import com.company.marketdataservice.catalog.registry.IngestProvider;
import com.company.marketdataservice.catalog.registry.QuoteCurrency;

import java.util.List;

/**
 * TCMB devlet tahvil getiri serileri (EVDS) ingest registry'sidir.
 */
public final class BondRegistry {

    private BondRegistry() {}

    private record BondRow(String symbol, String evdsSeries) {}

    private static final List<BondRow> ROWS = List.of(
            new BondRow("TRBOND1Y", "TP.KTF10"),
            new BondRow("TRBOND2Y", "TP.KTF11"),
            new BondRow("TRBOND3Y", "TP.KTF12"));

    public static List<IngestInstrumentDef> all() {
        return ROWS.stream().map(BondRegistry::def).toList();
    }

    public static List<BondIngestRow> ingestRows() {
        return ROWS.stream()
                .map(row -> new BondIngestRow(row.symbol(), row.evdsSeries()))
                .toList();
    }

    public record BondIngestRow(String symbol, String evdsSeries) {}

    private static IngestInstrumentDef def(BondRow row) {
        return new IngestInstrumentDef(
                row.symbol(),
                "TR Government Bond " + row.symbol(),
                AssetKind.BOND,
                "TCMB",
                QuoteCurrency.TRY,
                IngestProvider.TCMB_BOND,
                row.symbol(),
                null,
                row.evdsSeries(),
                null
        );
    }
}
