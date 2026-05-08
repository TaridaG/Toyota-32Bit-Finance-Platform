package com.company.marketdataservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.marketdataservice.dto.InstrumentFundamentalsDto;
import com.company.marketdataservice.fundamentals.InstrumentFundamentalsCacheEntry;
import com.company.marketdataservice.fundamentals.InstrumentFundamentalsCacheRepository;
import com.company.marketdataservice.fundamentals.provider.InstrumentFundamentalsProvider;
import com.company.marketdataservice.instrument.InstrumentCatalogEntry;
import com.company.marketdataservice.instrument.InstrumentCatalogRepository;
import com.company.marketdataservice.instrument.ProviderInstrumentMapping;
import com.company.marketdataservice.instrument.ProviderInstrumentMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class InstrumentFundamentalsServiceImpl implements InstrumentFundamentalsService {
    private static final Logger log = LoggerFactory.getLogger(InstrumentFundamentalsServiceImpl.class);
    private static final String PROVIDER_INTERNAL_META = "INTERNAL_META";

    private final InstrumentCatalogRepository instrumentCatalogRepository;
    private final ProviderInstrumentMappingRepository providerInstrumentMappingRepository;
    private final InstrumentFundamentalsCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, InstrumentFundamentalsProvider> providersByCode;

    @Value("${market.fundamentals.cache-ttl-hours:24}")
    private long cacheTtlHours;
    @Value("${market.fundamentals.refresh-once-per-year:true}")
    private boolean refreshOncePerYear;

    @Value("#{'${market.fundamentals.provider-order:FINNHUB,FMP,COINGECKO,INTERNAL_META}'.split(',')}")
    private List<String> providerOrder;

    public InstrumentFundamentalsServiceImpl(
            InstrumentCatalogRepository instrumentCatalogRepository,
            ProviderInstrumentMappingRepository providerInstrumentMappingRepository,
            InstrumentFundamentalsCacheRepository cacheRepository,
            ObjectMapper objectMapper,
            List<InstrumentFundamentalsProvider> providers
    ) {
        this.instrumentCatalogRepository = instrumentCatalogRepository;
        this.providerInstrumentMappingRepository = providerInstrumentMappingRepository;
        this.cacheRepository = cacheRepository;
        this.objectMapper = objectMapper;
        this.providersByCode = new LinkedHashMap<>();
        for (InstrumentFundamentalsProvider provider : providers) {
            this.providersByCode.put(provider.providerCode().toUpperCase(Locale.ROOT), provider);
        }
    }

    @Override
    @Transactional
    public InstrumentFundamentalsDto getFundamentals(String symbol, boolean forceRefresh) {
        String canonicalSymbol = normalizeSymbol(symbol);
        if (canonicalSymbol == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbol is blank");
        }
        InstrumentCatalogEntry instrument = instrumentCatalogRepository.findByCanonicalSymbolAndActiveTrue(canonicalSymbol)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Instrument not found in catalog: " + canonicalSymbol
                ));

        Optional<InstrumentFundamentalsCacheEntry> cachedOpt = cacheRepository.findById(instrument.getInstrumentId());
        if (!forceRefresh && cachedOpt.isPresent()) {
            InstrumentFundamentalsCacheEntry cached = cachedOpt.get();
            if (!refreshOncePerYear || isSameUtcYear(cached.getFetchedAt(), Instant.now())) {
                InstrumentFundamentalsDto cachedDto = deserialize(cached.getPayloadJson());
                if (!shouldRefreshDerivedStockMarketCap(instrument, cachedDto)) {
                    return cachedDto.withCacheHit(true);
                }
                log.info("FUNDAMENTALS_CACHE_REFRESH_REQUIRED symbol={} reason=missing_market_cap_for_stock provider={}",
                        instrument.getCanonicalSymbol(),
                        cachedDto.provider());
            }
        }

        InstrumentFundamentalsDto fresh = loadFromProviders(instrument);

        Instant now = Instant.now();
        InstrumentFundamentalsCacheEntry cacheEntry = cachedOpt.orElseGet(InstrumentFundamentalsCacheEntry::new);
        cacheEntry.setInstrumentId(instrument.getInstrumentId());
        cacheEntry.setCanonicalSymbol(canonicalSymbol);
        cacheEntry.setProvider(fresh.provider());
        cacheEntry.setProviderSymbol(fresh.providerSymbol());
        cacheEntry.setPayloadJson(serialize(fresh.withCacheHit(false)));
        cacheEntry.setFetchedAt(now);
        // Persist fundamentals as long-lived data; only refresh yearly (or forceRefresh).
        cacheEntry.setExpiresAt(now.plusSeconds(Math.max(cacheTtlHours, 24L * 365L) * 3600L));
        cacheRepository.save(cacheEntry);

        return fresh.withCacheHit(false);
    }

    private InstrumentFundamentalsDto loadFromProviders(InstrumentCatalogEntry instrument) {
        List<ProviderCandidate> candidates = buildProviderCandidates(instrument);
        for (ProviderCandidate candidate : candidates) {
            InstrumentFundamentalsProvider provider = providersByCode.get(candidate.providerCode);
            if (provider == null || !provider.supports(instrument)) {
                continue;
            }
            try {
                InstrumentFundamentalsDto dto = provider.fetch(instrument, candidate.providerSymbol);
                if (dto != null) {
                    return dto;
                }
            } catch (Exception ex) {
                log.warn("FUNDAMENTALS_PROVIDER_FAILED provider={} symbol={} reason={}",
                        candidate.providerCode,
                        instrument.getCanonicalSymbol(),
                        ex.getMessage());
            }
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No fundamentals provider available for symbol");
    }

    private List<ProviderCandidate> buildProviderCandidates(InstrumentCatalogEntry instrument) {
        List<ProviderCandidate> out = new ArrayList<>();
        List<ProviderInstrumentMapping> mappings = providerInstrumentMappingRepository
                .findByInstrumentIdAndActiveTrueOrderByPriorityAsc(instrument.getInstrumentId());
        for (ProviderInstrumentMapping mapping : mappings) {
            String providerCode = normalizeProviderCode(mapping.getProvider());
            String providerSymbol = normalizeSymbol(mapping.getProviderSymbol());
            if (providerCode == null || providerSymbol == null) {
                continue;
            }
            out.add(new ProviderCandidate(providerCode, providerSymbol));
        }
        for (String configuredProvider : providerOrder) {
            String providerCode = normalizeProviderCode(configuredProvider);
            if (providerCode == null || containsProvider(out, providerCode)) {
                continue;
            }
            out.add(new ProviderCandidate(providerCode, instrument.getCanonicalSymbol()));
        }
        if (!containsProvider(out, PROVIDER_INTERNAL_META)) {
            out.add(new ProviderCandidate(PROVIDER_INTERNAL_META, instrument.getCanonicalSymbol()));
        }
        return out;
    }

    private boolean containsProvider(List<ProviderCandidate> candidates, String providerCode) {
        for (ProviderCandidate c : candidates) {
            if (c.providerCode.equals(providerCode)) {
                return true;
            }
        }
        return false;
    }

    private String serialize(InstrumentFundamentalsDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize fundamentals cache payload", ex);
        }
    }

    private InstrumentFundamentalsDto deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, InstrumentFundamentalsDto.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to deserialize fundamentals cache payload", ex);
        }
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeProviderCode(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            return null;
        }
        return providerCode.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isSameUtcYear(Instant left, Instant right) {
        if (left == null || right == null) {
            return false;
        }
        int leftYear = left.atZone(ZoneOffset.UTC).getYear();
        int rightYear = right.atZone(ZoneOffset.UTC).getYear();
        return leftYear == rightYear;
    }

    private static boolean shouldRefreshDerivedStockMarketCap(
            InstrumentCatalogEntry instrument,
            InstrumentFundamentalsDto cachedDto
    ) {
        if (instrument == null || cachedDto == null) {
            return false;
        }
        String assetClass = instrument.getAssetClass();
        if (assetClass == null) {
            return false;
        }
        boolean missingMarketCap = cachedDto.marketCapitalization() == null;
        return ("STOCK".equalsIgnoreCase(assetClass) || "CRYPTO".equalsIgnoreCase(assetClass))
                && missingMarketCap;
    }

    private record ProviderCandidate(String providerCode, String providerSymbol) {
    }
}
