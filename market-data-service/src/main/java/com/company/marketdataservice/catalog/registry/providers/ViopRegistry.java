package com.company.marketdataservice.catalog.registry.providers;

import com.company.marketdataservice.catalog.registry.AssetKind;
import com.company.marketdataservice.catalog.registry.IngestInstrumentDef;
import com.company.marketdataservice.catalog.registry.IngestProvider;
import com.company.marketdataservice.catalog.registry.QuoteCurrency;
import com.company.marketdataservice.viop.application.ViopAliasResolver;
import java.util.List;

/**
 * faiz-vadeli UI tarafından kullanılan sabit VIOP alias tanımlarını tutan ingest registry'sidir.
 */
public final class ViopRegistry {

    private ViopRegistry() {}

    public static List<IngestInstrumentDef> all() {
        return List.of(
                def(ViopAliasResolver.ALIAS_TLREF_NEAR, "VIOP TLREF Near Contract"),
                def(ViopAliasResolver.ALIAS_DIBS_NEAR, "VIOP DIBS Near Contract"),
                def(ViopAliasResolver.ALIAS_FAIZ_NEAR, "VIOP Interest Near Contract"));
    }

    private static IngestInstrumentDef def(String symbol, String name) {
        return new IngestInstrumentDef(
                symbol,
                name,
                AssetKind.BOND,
                "BIST",
                QuoteCurrency.TRY,
                IngestProvider.BIST_DERIVATIVES,
                symbol,
                null,
                symbol,
                null);
    }
}

