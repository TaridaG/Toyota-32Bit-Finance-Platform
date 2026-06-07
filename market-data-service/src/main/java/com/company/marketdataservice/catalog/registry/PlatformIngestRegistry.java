package com.company.marketdataservice.catalog.registry;

import com.company.marketdataservice.catalog.registry.providers.BistRegistry;
import com.company.marketdataservice.catalog.registry.providers.BondRegistry;
import com.company.marketdataservice.catalog.registry.providers.CryptoRegistry;
import com.company.marketdataservice.catalog.registry.providers.EurobondRegistry;
import com.company.marketdataservice.catalog.registry.providers.FundRegistry;
import com.company.marketdataservice.catalog.registry.providers.FxRegistry;
import com.company.marketdataservice.catalog.registry.providers.NasdaqRegistry;
import com.company.marketdataservice.catalog.registry.providers.ViopRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Platform ingest enstrüman tanımları için tek kaynak (single source of truth) registry'sidir.
 */
public final class PlatformIngestRegistry {

    private static final List<IngestInstrumentDef> ALL = buildAll();

    private PlatformIngestRegistry() {}

    public static List<IngestInstrumentDef> all() {
        return ALL;
    }

    public static List<IngestInstrumentDef> byKind(AssetKind kind) {
        if (kind == null) {
            return List.of();
        }
        return ALL.stream().filter(def -> def.kind() == kind).toList();
    }

    public static List<String> symbolsByKind(AssetKind kind) {
        return byKind(kind).stream().map(IngestInstrumentDef::symbol).toList();
    }

    /** Kripto + BIST + US stock/ETF tanımları (stock scheduler poll evreni). */
    public static List<IngestInstrumentDef> polledEquities() {
        List<IngestInstrumentDef> out = new ArrayList<>();
        out.addAll(BistRegistry.all());
        out.addAll(NasdaqRegistry.all());
        return List.copyOf(out);
    }

    public static List<String> polledEquitySymbols() {
        return polledEquities().stream().map(IngestInstrumentDef::symbol).toList();
    }

    public static String coingeckoIdForBase(String baseAsset) {
        return CryptoRegistry.coingeckoIdForBase(baseAsset);
    }

    private static List<IngestInstrumentDef> buildAll() {
        List<IngestInstrumentDef> merged = new ArrayList<>();
        merged.addAll(CryptoRegistry.all());
        merged.addAll(BistRegistry.all());
        merged.addAll(NasdaqRegistry.all());
        merged.addAll(FundRegistry.all());
        merged.addAll(BondRegistry.all());
        merged.addAll(ViopRegistry.all());
        merged.addAll(FxRegistry.all());
        merged.addAll(EurobondRegistry.all());
        assertNoDuplicateSymbols(merged);
        return List.copyOf(merged);
    }

    private static void assertNoDuplicateSymbols(List<IngestInstrumentDef> defs) {
        Set<String> seen = new HashSet<>();
        for (IngestInstrumentDef def : defs) {
            String key = def.symbol().trim().toUpperCase(Locale.ROOT);
            if (!seen.add(key)) {
                throw new IllegalStateException("Duplicate registry symbol: " + def.symbol());
            }
        }
    }
}
