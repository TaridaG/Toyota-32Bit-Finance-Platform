package com.company.marketdataservice.catalog.application;

import com.company.marketdataservice.bootstrap.config.FinnhubProperties;
import com.company.marketdataservice.catalog.domain.IngestScopeSegment;
import com.company.marketdataservice.catalog.registry.IngestInstrumentDef;
import com.company.marketdataservice.catalog.registry.IngestProvider;
import com.company.marketdataservice.catalog.registry.PlatformIngestRegistry;
import com.company.marketdataservice.catalog.registry.providers.BistRegistry;
import com.company.marketdataservice.catalog.registry.providers.CryptoRegistry;
import com.company.marketdataservice.catalog.registry.providers.FundRegistry;
import com.company.marketdataservice.catalog.registry.providers.NasdaqRegistry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Facade over {@link PlatformIngestRegistry} for schedulers and orchestrators.
 */
@Service
public class InstrumentIngestScopeService {

    public List<String> symbolsForSegment(IngestScopeSegment segment) {
        if (segment == null) {
            return List.of();
        }
        return switch (segment) {
            case BIST -> BistRegistry.all().stream().map(IngestInstrumentDef::symbol).toList();
            case NASDAQ -> NasdaqRegistry.all().stream().map(IngestInstrumentDef::symbol).toList();
            case CRYPTO -> PlatformIngestRegistry.symbolsByKind(com.company.marketdataservice.catalog.registry.AssetKind.CRYPTO);
            case ALL_STOCKS -> resolveTrackedStockSymbols();
        };
    }

    public List<String> resolveTrackedCryptoSymbols() {
        return PlatformIngestRegistry.symbolsByKind(com.company.marketdataservice.catalog.registry.AssetKind.CRYPTO);
    }

    public List<String> resolveTrackedStockSymbols() {
        return PlatformIngestRegistry.polledEquitySymbols();
    }

    public List<IngestInstrumentDef> resolvePolledEquityDefinitions() {
        return PlatformIngestRegistry.polledEquities();
    }

    public List<String> resolveTrackedFundCodes() {
        return FundRegistry.tefasCodes();
    }

    /**
     * Whether live stock ingest should use Finnhub for this canonical symbol.
     */
    public boolean isFinnhubOwned(String symbol, FinnhubProperties finnhubProperties) {
        if (symbol == null || symbol.isBlank() || finnhubProperties == null || !finnhubProperties.isEnabled()) {
            return false;
        }
        return NasdaqRegistry.isFinnhubOwned(symbol);
    }

    public boolean usesFinnhubProvider(IngestInstrumentDef def) {
        return def != null && def.provider() == IngestProvider.FINNHUB;
    }

    public String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return "";
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
