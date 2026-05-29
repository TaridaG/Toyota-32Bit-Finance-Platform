package com.company.marketdataservice.catalog.registry;

import com.company.marketdataservice.catalog.registry.providers.BistRegistry;
import com.company.marketdataservice.catalog.registry.providers.BondRegistry;
import com.company.marketdataservice.catalog.registry.providers.CryptoRegistry;
import com.company.marketdataservice.catalog.registry.providers.EurobondRegistry;
import com.company.marketdataservice.catalog.registry.providers.FundRegistry;
import com.company.marketdataservice.catalog.registry.providers.FxRegistry;
import com.company.marketdataservice.catalog.registry.providers.NasdaqRegistry;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformIngestRegistryTest {

    @Test
    void all_containsExpectedSegmentCounts() {
        int expected = CryptoRegistry.all().size()
                + BistRegistry.all().size()
                + NasdaqRegistry.all().size()
                + FundRegistry.all().size()
                + BondRegistry.all().size()
                + FxRegistry.all().size()
                + EurobondRegistry.all().size();
        assertEquals(expected, PlatformIngestRegistry.all().size());
    }

    @Test
    void all_hasNoDuplicateSymbols() {
        List<IngestInstrumentDef> all = PlatformIngestRegistry.all();
        Set<String> seen = new HashSet<>();
        for (IngestInstrumentDef def : all) {
            assertTrue(seen.add(def.symbol().toUpperCase()), "duplicate: " + def.symbol());
        }
    }

    @Test
    void all_excludesMetalFutures() {
        assertTrue(PlatformIngestRegistry.all().stream()
                .map(IngestInstrumentDef::symbol)
                .noneMatch(s -> s.contains("=F")));
    }

    @Test
    void byKind_filtersCrypto() {
        List<IngestInstrumentDef> crypto = PlatformIngestRegistry.byKind(AssetKind.CRYPTO);
        assertEquals(CryptoRegistry.SYMBOLS.size(), crypto.size());
        assertTrue(crypto.stream().allMatch(def -> def.kind() == AssetKind.CRYPTO));
    }

    @Test
    void polledEquities_excludesCryptoAndBonds() {
        List<String> symbols = PlatformIngestRegistry.polledEquitySymbols();
        assertTrue(symbols.contains("GARAN"));
        assertTrue(symbols.contains("AAPL"));
        assertTrue(symbols.contains("SPY"));
        assertFalse(symbols.contains("BTCUSDT"));
        assertFalse(symbols.contains("TRBOND5Y"));
    }

    @Test
    void bondRegistry_evdsSeriesPresent() {
        assertTrue(BondRegistry.all().stream()
                .allMatch(def -> def.evdsSeries() != null && !def.evdsSeries().isBlank()));
    }

    @Test
    void eurobondRegistry_yahooSymbolsPresent() {
        assertTrue(EurobondRegistry.all().stream()
                .allMatch(def -> def.yahooChartSymbol() != null && def.yahooChartSymbol().contains(":GOV")));
    }
}
