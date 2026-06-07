package com.company.marketdataservice.catalog.infrastructure.service;

import com.company.marketdataservice.catalog.infrastructure.http.dto.IngestCatalogItemDto;
import com.company.marketdataservice.catalog.infrastructure.http.dto.IngestCatalogPageDto;
import com.company.marketdataservice.catalog.infrastructure.persistence.IngestConfigEntry;
import com.company.marketdataservice.catalog.infrastructure.persistence.IngestConfigRepository;
import com.company.marketdataservice.catalog.infrastructure.persistence.InstrumentCatalogEntry;
import com.company.marketdataservice.catalog.infrastructure.persistence.InstrumentCatalogRepository;
import com.company.marketdataservice.catalog.application.InstrumentMappingService;
import com.company.marketdataservice.catalog.infrastructure.persistence.ProviderInstrumentMapping;
import com.company.marketdataservice.catalog.infrastructure.persistence.ProviderInstrumentMappingRepository;
import com.company.marketdataservice.bootstrap.config.FinnhubProperties;
import com.company.marketdataservice.history.infrastructure.orchestration.HistoricalBackfillService;
import com.company.marketdataservice.history.infrastructure.write.MarketHistoryWriteService;
import com.company.marketdataservice.spot.domain.MarketPriceUpdatedEvent;
import com.company.marketdataservice.spot.infrastructure.provider.PriceProvider;
import com.company.marketdataservice.spot.infrastructure.provider.finnhub.FinnhubClient;
import com.company.marketdataservice.spot.infrastructure.provider.yahoo.YahooFinanceProvider;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/**
 * Ingest kataloğu görüntüleme, enable/disable ve manuel pull/delete aksiyonlarını yöneten
 * application katmanı use-case servisi.
 */
@Service
@Transactional(readOnly = true)
public class IngestCatalogAdminService {
    private static final Logger log = LoggerFactory.getLogger(IngestCatalogAdminService.class);
    private static final int CATALOG_PAGE_SIZE_MAX = 50;
    private static final Set<String> ALLOWED_SEGMENTS = Set.of("CRYPTO", "BIST", "NASDAQ");
    private static final Set<String> NASDAQ_EXCHANGES = Set.of("NASDAQ", "FINNHUB", "YAHOO");

    private final IngestConfigRepository ingestConfigRepository;
    private final InstrumentCatalogRepository instrumentCatalogRepository;
    private final ProviderInstrumentMappingRepository providerInstrumentMappingRepository;
    private final MarketPriceHistoryRepository marketPriceHistoryRepository;
    private final HistoricalBackfillService historicalBackfillService;
    private final MarketHistoryWriteService marketHistoryWriteService;
    private final InstrumentMappingService instrumentMappingService;
    private final PriceProvider priceProvider;
    private final YahooFinanceProvider yahooFinanceProvider;
    private final FinnhubClient finnhubClient;
    private final FinnhubProperties finnhubProperties;
    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String financeApiBaseUrl;

    public IngestCatalogAdminService(
            IngestConfigRepository ingestConfigRepository,
            InstrumentCatalogRepository instrumentCatalogRepository,
            ProviderInstrumentMappingRepository providerInstrumentMappingRepository,
            MarketPriceHistoryRepository marketPriceHistoryRepository,
            HistoricalBackfillService historicalBackfillService,
            MarketHistoryWriteService marketHistoryWriteService,
            InstrumentMappingService instrumentMappingService,
            PriceProvider priceProvider,
            YahooFinanceProvider yahooFinanceProvider,
            FinnhubClient finnhubClient,
            FinnhubProperties finnhubProperties,
            JdbcTemplate jdbcTemplate,
            MeterRegistry meterRegistry,
            @Value("${clients.finance-api.base-url:http://finance-api:8080}") String financeApiBaseUrl
    ) {
        this.ingestConfigRepository = ingestConfigRepository;
        this.instrumentCatalogRepository = instrumentCatalogRepository;
        this.providerInstrumentMappingRepository = providerInstrumentMappingRepository;
        this.marketPriceHistoryRepository = marketPriceHistoryRepository;
        this.historicalBackfillService = historicalBackfillService;
        this.marketHistoryWriteService = marketHistoryWriteService;
        this.instrumentMappingService = instrumentMappingService;
        this.priceProvider = priceProvider;
        this.yahooFinanceProvider = yahooFinanceProvider;
        this.finnhubClient = finnhubClient;
        this.finnhubProperties = finnhubProperties;
        this.jdbcTemplate = jdbcTemplate;
        this.meterRegistry = meterRegistry;
        this.financeApiBaseUrl = financeApiBaseUrl;
    }

    /**
     * Ingest config kayıtlarını sayfalı olarak katalog metadata ve geçmiş kapsam metrikleriyle birleştirip döner.
     */
    public IngestCatalogPageDto getCatalogPage(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), CATALOG_PAGE_SIZE_MAX);
        Page<IngestConfigEntry> configPage = ingestConfigRepository.findAllByOrderBySegmentAscInstrumentIdAsc(
                PageRequest.of(safePage, safeSize)
        );
        List<IngestCatalogItemDto> content = mapConfigsToCatalogItems(configPage.getContent());
        return new IngestCatalogPageDto(
                content,
                configPage.getTotalElements(),
                configPage.getTotalPages(),
                configPage.getNumber(),
                configPage.getSize()
        );
    }

    private List<IngestCatalogItemDto> mapConfigsToCatalogItems(List<IngestConfigEntry> configs) {
        if (configs == null || configs.isEmpty()) {
            return List.of();
        }
        List<Long> instrumentIds = configs.stream().map(IngestConfigEntry::getInstrumentId).distinct().toList();
        Map<Long, InstrumentCatalogEntry> catalogById = new HashMap<>();
        for (InstrumentCatalogEntry entry : instrumentCatalogRepository.findAllById(instrumentIds)) {
            catalogById.put(entry.getInstrumentId(), entry);
        }
        List<IngestCatalogItemDto> out = new ArrayList<>();
        Instant now = Instant.now();
        Instant recentFrom = now.minus(365, ChronoUnit.DAYS);
        Instant recent30From = now.minus(30, ChronoUnit.DAYS);
        for (IngestConfigEntry cfg : configs) {
            InstrumentCatalogEntry cat = catalogById.get(cfg.getInstrumentId());
            if (cat == null) {
                meterRegistry.counter(
                        "market_data_ingest_catalog_missing_catalog_row_total",
                        "service", "market-data-service",
                        "segment", cfg.getSegment()
                ).increment();
                log.warn("INGEST_CATALOG_MISSING_CATALOG instrumentId={} segment={}",
                        cfg.getInstrumentId(), cfg.getSegment());
            }
            String symbol = cat != null ? cat.getCanonicalSymbol() : ("instrument-" + cfg.getInstrumentId());
            Long totalDays = cat != null ? marketPriceHistoryRepository.countDistinctDaysBySymbol(symbol) : null;
            Long recentDays = cat != null ? marketPriceHistoryRepository.countDistinctDaysBySymbolSince(symbol, recentFrom) : null;
            Long recent30Days = cat != null ? marketPriceHistoryRepository.countDistinctDaysBySymbolSince(symbol, recent30From) : null;
            out.add(new IngestCatalogItemDto(
                    cfg.getInstrumentId(),
                    symbol,
                    cat != null ? cat.getAssetClass() : null,
                    null,
                    cfg.getSegment(),
                    cfg.isEnabled(),
                    totalDays,
                    recentDays,
                    recent30Days,
                    cfg.getLastError(),
                    cfg.getLastErrorAt()
            ));
        }
        return out;
    }

    @Transactional
    public void enable(Long instrumentId, String segmentRaw) {
        if (instrumentId == null || segmentRaw == null || segmentRaw.isBlank()) {
            return;
        }
        String segment = segmentRaw.trim().toUpperCase(Locale.ROOT);
        validateSegment(segment);
        FinanceInstrument financeInstrument = loadFinanceInstrument(instrumentId);
        validateSegmentCompatibility(segment, financeInstrument);
        ensureCatalog(financeInstrument);
        ensureProviderMapping(financeInstrument, segment);

        IngestConfigEntry.IngestConfigId id = new IngestConfigEntry.IngestConfigId(instrumentId, segment);
        IngestConfigEntry entry = ingestConfigRepository.findById(id).orElseGet(() -> {
            IngestConfigEntry e = new IngestConfigEntry();
            e.setInstrumentId(instrumentId);
            e.setSegment(segment);
            return e;
        });
        entry.setEnabled(true);
        ingestConfigRepository.save(entry);
    }

    @Transactional
    public void disable(Long instrumentId, String segmentRaw) {
        if (instrumentId == null || segmentRaw == null || segmentRaw.isBlank()) {
            return;
        }
        String segment = segmentRaw.trim().toUpperCase(Locale.ROOT);
        validateSegment(segment);
        IngestConfigEntry.IngestConfigId id = new IngestConfigEntry.IngestConfigId(instrumentId, segment);
        ingestConfigRepository.findById(id).ifPresent(entry -> {
            entry.setEnabled(false);
            ingestConfigRepository.save(entry);
        });
    }

    @Transactional
    public void triggerHistoryPull(Long instrumentId, String segmentRaw) {
        if (instrumentId == null || segmentRaw == null || segmentRaw.isBlank()) {
            return;
        }
        String segment = segmentRaw.trim().toUpperCase(Locale.ROOT);
        validateSegment(segment);
        FinanceInstrument instrument = loadFinanceInstrument(instrumentId);
        validateSegmentCompatibility(segment, instrument);
        ensureCatalog(instrument);
        ensureProviderMapping(instrument, segment);
        historicalBackfillService.backfillPrice(instrument.symbol().trim().toUpperCase(Locale.ROOT));
    }

    @Transactional
    public void triggerLivePull(Long instrumentId, String segmentRaw) {
        if (instrumentId == null || segmentRaw == null || segmentRaw.isBlank()) {
            return;
        }
        String segment = segmentRaw.trim().toUpperCase(Locale.ROOT);
        validateSegment(segment);
        FinanceInstrument instrument = loadFinanceInstrument(instrumentId);
        validateSegmentCompatibility(segment, instrument);
        ensureCatalog(instrument);
        ensureProviderMapping(instrument, segment);

        String symbol = instrument.symbol().trim().toUpperCase(Locale.ROOT);
        String source;
        BigDecimal price;
        if ("CRYPTO".equals(segment)) {
            source = priceProvider.source();
            price = priceProvider.fetchPrice(symbol);
        } else if ("NASDAQ".equals(segment) && finnhubProperties.isEnabled() && finnhubProperties.ownsSymbol(symbol)) {
            source = "FINNHUB";
            price = finnhubClient.fetchLiveQuotePrice(symbol);
        } else {
            source = yahooFinanceProvider.source();
            price = yahooFinanceProvider.fetchPrice(symbol);
        }
        Long resolvedInstrumentId = instrumentMappingService.resolveInstrument(source, symbol).orElse(instrumentId);
        marketHistoryWriteService.save(MarketPriceUpdatedEvent.of(symbol, price, "MARKET", source, resolvedInstrumentId));
    }

    @Transactional
    public void deleteInstrumentFromIngest(Long instrumentId, String segmentRaw) {
        if (instrumentId == null || segmentRaw == null || segmentRaw.isBlank()) {
            return;
        }
        String segment = segmentRaw.trim().toUpperCase(Locale.ROOT);
        validateSegment(segment);
        IngestConfigEntry.IngestConfigId id = new IngestConfigEntry.IngestConfigId(instrumentId, segment);
        ingestConfigRepository.deleteById(id);

        FinanceInstrument instrument = loadFinanceInstrument(instrumentId);
        String symbol = instrument.symbol().trim().toUpperCase(Locale.ROOT);
        jdbcTemplate.update("DELETE FROM mds_market_price_history WHERE instrument_symbol = ?", symbol);

        long remainingConfigs = ingestConfigRepository.countByInstrumentId(instrumentId);
        if (remainingConfigs == 0) {
            jdbcTemplate.update("DELETE FROM mds_provider_instrument_mapping WHERE instrument_id = ?", instrumentId);
            instrumentCatalogRepository.deleteById(instrumentId);
            deactivateFinanceInstrument(instrumentId);
        }
    }

    private void deactivateFinanceInstrument(Long instrumentId) {
        String url = financeApiBaseUrl + "/api/v1/admin/instruments/" + instrumentId + "/deactivate";
        try {
            restTemplate.postForObject(url, null, Void.class);
        } catch (Exception ex) {
            log.warn("FINANCE_INSTRUMENT_DEACTIVATE_FAILED instrumentId={} reason={}", instrumentId, ex.getMessage());
            throw new IllegalStateException("Finance instrument deactivate failed for id=" + instrumentId, ex);
        }
    }

    private void validateSegment(String segment) {
        if (!ALLOWED_SEGMENTS.contains(segment)) {
            throw new IllegalArgumentException("segment must be one of CRYPTO, BIST, NASDAQ.");
        }
    }

    private void validateSegmentCompatibility(String segment, FinanceInstrument instrument) {
        String type = instrument.type().toUpperCase(Locale.ROOT);
        String exchange = instrument.exchange().toUpperCase(Locale.ROOT);
        if ("CRYPTO".equals(segment)) {
            if (!"CRYPTO".equals(type)) {
                throw new IllegalArgumentException("CRYPTO segment requires instrument type CRYPTO.");
            }
            if (!"BINANCE".equals(exchange)) {
                throw new IllegalArgumentException("CRYPTO segment requires exchange BINANCE.");
            }
            return;
        }
        if (!"STOCK".equals(type)) {
            throw new IllegalArgumentException(segment + " segment requires instrument type STOCK.");
        }
        if ("BIST".equals(segment) && !"BIST".equals(exchange)) {
            throw new IllegalArgumentException("BIST segment requires exchange BIST.");
        }
        if ("NASDAQ".equals(segment) && !NASDAQ_EXCHANGES.contains(exchange)) {
            throw new IllegalArgumentException("NASDAQ segment requires exchange in [NASDAQ, FINNHUB, YAHOO].");
        }
    }

    private FinanceInstrument loadFinanceInstrument(Long instrumentId) {
        List<FinanceInstrument> rows = jdbcTemplate.query(
                """
                        SELECT id, symbol, type, exchange, active
                        FROM instruments
                        WHERE id = ?
                        """,
                (rs, rowNum) -> new FinanceInstrument(
                        rs.getLong("id"),
                        rs.getString("symbol"),
                        rs.getString("type"),
                        rs.getString("exchange"),
                        rs.getBoolean("active")
                ),
                instrumentId
        );
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Instrument not found for id=" + instrumentId);
        }
        FinanceInstrument instrument = rows.get(0);
        if (!instrument.active()) {
            throw new IllegalArgumentException("Instrument is inactive for id=" + instrumentId);
        }
        return instrument;
    }

    private void ensureCatalog(FinanceInstrument instrument) {
        InstrumentCatalogEntry entry = instrumentCatalogRepository.findById(instrument.id()).orElseGet(() -> {
            InstrumentCatalogEntry created = new InstrumentCatalogEntry();
            created.setInstrumentId(instrument.id());
            return created;
        });
        entry.setCanonicalSymbol(instrument.symbol().trim().toUpperCase(Locale.ROOT));
        entry.setAssetClass(instrument.type().trim().toUpperCase(Locale.ROOT));
        entry.setBaseCurrency(resolveBaseCurrency(instrument));
        entry.setQuoteCurrency(resolveQuoteCurrency(instrument));
        entry.setActive(true);
        instrumentCatalogRepository.save(entry);
    }

    private void ensureProviderMapping(FinanceInstrument instrument, String segment) {
        String canonicalSymbol = instrument.symbol().trim().toUpperCase(Locale.ROOT);
        String provider;
        String providerSymbol;
        if ("CRYPTO".equals(segment)) {
            provider = "BINANCE";
            providerSymbol = canonicalSymbol;
        } else if ("BIST".equals(segment)) {
            provider = "YAHOO";
            providerSymbol = canonicalSymbol + ".IS";
        } else {
            provider = "FINNHUB";
            providerSymbol = canonicalSymbol;
        }
        ProviderInstrumentMapping mapping = providerInstrumentMappingRepository
                .findFirstByProviderIgnoreCaseAndProviderSymbolOrderByPriorityAsc(provider, providerSymbol)
                .orElseGet(ProviderInstrumentMapping::new);
        mapping.setProvider(provider);
        mapping.setProviderSymbol(providerSymbol);
        mapping.setInstrumentId(instrument.id());
        mapping.setPriority(0);
        mapping.setActive(true);
        providerInstrumentMappingRepository.save(mapping);
    }

    private static String resolveQuoteCurrency(FinanceInstrument instrument) {
        String type = instrument.type().trim().toUpperCase(Locale.ROOT);
        if ("CRYPTO".equals(type) && instrument.symbol().toUpperCase(Locale.ROOT).endsWith("USDT")) {
            return "USDT";
        }
        if ("STOCK".equals(type) && "BIST".equalsIgnoreCase(instrument.exchange())) {
            return "TRY";
        }
        if ("STOCK".equals(type)) {
            return "USD";
        }
        return null;
    }

    private static String resolveBaseCurrency(FinanceInstrument instrument) {
        String symbol = instrument.symbol().trim().toUpperCase(Locale.ROOT);
        String type = instrument.type().trim().toUpperCase(Locale.ROOT);
        if ("CRYPTO".equals(type) && symbol.endsWith("USDT") && symbol.length() > 4) {
            return symbol.substring(0, symbol.length() - 4);
        }
        return null;
    }

    private record FinanceInstrument(Long id, String symbol, String type, String exchange, boolean active) {
    }
}

