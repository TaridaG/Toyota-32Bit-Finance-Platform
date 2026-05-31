package com.company.marketdataservice.catalog.registry.providers;

import com.company.marketdataservice.catalog.registry.AssetKind;
import com.company.marketdataservice.catalog.registry.IngestInstrumentDef;
import com.company.marketdataservice.catalog.registry.IngestProvider;
import com.company.marketdataservice.catalog.registry.QuoteCurrency;

import java.util.List;

/**
 * Turkish TEFAS fund codes; canonical symbol is {@code FUND_{code}}.
 */
public final class FundRegistry {

    private FundRegistry() {}

    private static final List<String> TEFAS_CODES = List.of("TI2", "TP2", "AFT", "AFA", "AFO", "GTA");

    public static List<IngestInstrumentDef> all() {
        return TEFAS_CODES.stream().map(FundRegistry::def).toList();
    }

    public static List<String> tefasCodes() {
        return TEFAS_CODES;
    }

    private static IngestInstrumentDef def(String tefasCode) {
        String canonical = "FUND_" + tefasCode;
        return new IngestInstrumentDef(
                canonical,
                "TEFAS " + tefasCode,
                AssetKind.FUND,
                "TEFAS",
                QuoteCurrency.TRY,
                IngestProvider.TEFAS,
                tefasCode,
                tefasCode,
                null,
                null
        );
    }
}
