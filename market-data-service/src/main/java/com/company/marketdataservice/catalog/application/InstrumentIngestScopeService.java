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
import com.company.marketdataservice.catalog.infrastructure.persistence.IngestConfigEntry;
import com.company.marketdataservice.catalog.infrastructure.persistence.IngestConfigRepository;
import com.company.marketdataservice.catalog.infrastructure.persistence.InstrumentCatalogEntry;
import com.company.marketdataservice.catalog.infrastructure.persistence.InstrumentCatalogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Scheduler ve orchestrator'lar için {@link PlatformIngestRegistry} üzerinde facade servisidir.
 */
@Service
public class InstrumentIngestScopeService {

    private final IngestConfigRepository ingestConfigRepository;
    private final InstrumentCatalogRepository instrumentCatalogRepository;

    public InstrumentIngestScopeService(
            IngestConfigRepository ingestConfigRepository,
            InstrumentCatalogRepository instrumentCatalogRepository
    ) {
        this.ingestConfigRepository = ingestConfigRepository;
        this.instrumentCatalogRepository = instrumentCatalogRepository;
    }

    public List<String> symbolsForSegment(IngestScopeSegment segment) {
        if (segment == null) {
            return List.of();
        }
        return switch (segment) {
            case BIST -> resolveSymbolsForSegmentWithFallback("BIST",
                    BistRegistry.all().stream().map(IngestInstrumentDef::symbol).toList());
            case NASDAQ -> resolveSymbolsForSegmentWithFallback("NASDAQ",
                    NasdaqRegistry.all().stream().map(IngestInstrumentDef::symbol).toList());
            case CRYPTO -> resolveTrackedCryptoSymbols();
            case ALL_STOCKS -> resolveTrackedStockSymbols();
        };
    }

    public List<String> resolveTrackedCryptoSymbols() {
        List<String> configured = resolveSymbolsForSegment("CRYPTO");
        if (!configured.isEmpty()) {
            return configured;
        }
        // Fallback to legacy registry when no DB-backed config exists.
        return PlatformIngestRegistry.symbolsByKind(com.company.marketdataservice.catalog.registry.AssetKind.CRYPTO);
    }

    public List<String> resolveTrackedStockSymbols() {
        List<String> bist = resolveSymbolsForSegment("BIST");
        List<String> nasdaq = resolveSymbolsForSegment("NASDAQ");
        if (!bist.isEmpty() || !nasdaq.isEmpty()) {
            return List.copyOf(
                    java.util.stream.Stream.concat(bist.stream(), nasdaq.stream())
                            .distinct()
                            .toList());
        }
        // Fallback to legacy registry when no DB-backed config exists.
        return PlatformIngestRegistry.polledEquitySymbols();
    }

    public List<IngestInstrumentDef> resolvePolledEquityDefinitions() {
        return PlatformIngestRegistry.polledEquities();
    }

    public List<String> resolveTrackedFundCodes() {
        return FundRegistry.tefasCodes();
    }

    /**
     * Bu canonical symbol için canlı stock ingest'in Finnhub kullanıp kullanmayacağını döner.
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

    private List<String> resolveSymbolsForSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            return List.of();
        }
        List<IngestConfigEntry> entries = ingestConfigRepository.findBySegmentAndEnabledTrue(segment.trim().toUpperCase(Locale.ROOT));
        if (entries.isEmpty()) {
            return List.of();
        }
        Map<Long, InstrumentCatalogEntry> catalogById = instrumentCatalogRepository.findAll().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        InstrumentCatalogEntry::getInstrumentId,
                        e -> e,
                        (a, b) -> a
                ));
        return entries.stream()
                .map(e -> catalogById.get(e.getInstrumentId()))
                .filter(Objects::nonNull)
                .map(InstrumentCatalogEntry::getCanonicalSymbol)
                .filter(Objects::nonNull)
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private List<String> resolveSymbolsForSegmentWithFallback(String segment, List<String> fallback) {
        List<String> configured = resolveSymbolsForSegment(segment);
        if (!configured.isEmpty()) {
            return configured;
        }
        return fallback == null ? List.of() : List.copyOf(fallback);
    }
}
