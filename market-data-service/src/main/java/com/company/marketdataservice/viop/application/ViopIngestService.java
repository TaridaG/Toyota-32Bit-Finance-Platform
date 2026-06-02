package com.company.marketdataservice.viop.application;

import com.company.marketdataservice.bootstrap.config.ViopMarketProperties;
import com.company.marketdataservice.catalog.application.InstrumentMappingService;
import com.company.marketdataservice.history.infrastructure.write.MarketHistoryWriteService;
import com.company.marketdataservice.spot.domain.MarketPriceUpdatedEvent;
import com.company.marketdataservice.viop.domain.ViopAliasSnapshot;
import com.company.marketdataservice.viop.domain.ViopContractRow;
import com.company.marketdataservice.viop.domain.ViopSettlementRow;
import com.company.marketdataservice.viop.infrastructure.persistence.ViopContractJdbcRepository;
import com.company.marketdataservice.viop.infrastructure.persistence.ViopIngestRunJdbcRepository;
import com.company.marketdataservice.viop.infrastructure.persistence.ViopSettlementJdbcRepository;
import com.company.marketdataservice.viop.infrastructure.source.BistDerivativesFileClient;
import com.company.marketdataservice.viop.infrastructure.source.BistDerivativesFileClient.DownloadedFile;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViopIngestService {

    private static final String SOURCE = "BIST_DERIVATIVES";
    private static final String PRICE_TYPE = "MARKET";

    private final ViopMarketProperties properties;
    private final BistDerivativesFileClient fileClient;
    private final ViopContractJdbcRepository contractRepository;
    private final ViopSettlementJdbcRepository settlementRepository;
    private final ViopIngestRunJdbcRepository runRepository;
    private final ViopAliasResolver aliasResolver;
    private final InstrumentMappingService instrumentMappingService;
    private final MarketHistoryWriteService marketHistoryWriteService;

    public void ingestDaily() {
        long runId = runRepository.start(SOURCE);
        try {
            LocalDate tradeDate = LocalDate.now(resolveZone());
            List<ViopContractRow> contracts = loadContracts();
            List<ViopSettlementRow> settlements = loadSettlements(tradeDate);

            if (!contracts.isEmpty()) {
                contractRepository.upsertAll(contracts);
                Set<String> active = new LinkedHashSet<>();
                for (ViopContractRow c : contracts) {
                    active.add(c.contractCode());
                }
                contractRepository.markMissingAsInactive(new ArrayList<>(active));
            }
            if (!settlements.isEmpty()) {
                settlementRepository.upsertAll(settlements);
            }

            List<ViopAliasSnapshot> aliases = aliasResolver.resolve(contracts, settlements, tradeDate);
            int aliasesWritten = writeAliasHistory(aliases);

            runRepository.finishSuccess(runId, contracts.size(), settlements.size(), aliasesWritten);
            log.info(
                    "VIOP_INGEST_DONE contracts={} settlements={} aliases={} tradeDate={}",
                    contracts.size(),
                    settlements.size(),
                    aliasesWritten,
                    tradeDate);
        } catch (Exception ex) {
            runRepository.finishFailure(runId, ex.toString());
            log.warn("VIOP_INGEST_FAILED reason={}", ex.toString(), ex);
        }
    }

    private List<ViopContractRow> loadContracts() throws Exception {
        List<String> candidates = resolveContractUrls();
        if (candidates.isEmpty()) {
            throw new IllegalStateException(
                    "VIOP source misconfigured: no contracts URL and no contracts template generated candidate URLs");
        }
        List<ViopContractRow> out = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (String url : candidates) {
            try {
                DownloadedFile file = fileClient.fetch(url.trim());
                List<ViopContractRow> parsed = ViopFileParsers.parseContracts(file);
                if (!parsed.isEmpty()) {
                    out.addAll(parsed);
                    log.info("VIOP_CONTRACT_SOURCE_OK url={} rows={}", url, parsed.size());
                    break;
                }
                failures.add(url + " -> empty");
            } catch (Exception ex) {
                failures.add(url + " -> " + ex.getMessage());
                log.debug("VIOP_CONTRACT_SOURCE_FAIL url={} reason={}", url, ex.toString());
            }
            sleepQuietly();
        }
        if (out.isEmpty()) {
            throw new IOException("No contract rows parsed from any source. attempts=" + failures);
        }
        return dedupeContracts(out);
    }

    private List<ViopSettlementRow> loadSettlements(LocalDate fallbackTradeDate) throws Exception {
        List<String> candidates = resolveSettlementUrls(fallbackTradeDate);
        if (candidates.isEmpty()) {
            throw new IllegalStateException(
                    "VIOP source misconfigured: no settlement URL and no settlement template generated candidate URLs");
        }
        List<ViopSettlementRow> out = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (String url : candidates) {
            try {
                DownloadedFile file = fileClient.fetch(url.trim());
                List<ViopSettlementRow> parsed = ViopFileParsers.parseSettlements(file, fallbackTradeDate);
                if (!parsed.isEmpty()) {
                    out.addAll(parsed);
                    log.info("VIOP_SETTLEMENT_SOURCE_OK url={} rows={}", url, parsed.size());
                    break;
                }
                failures.add(url + " -> empty");
            } catch (Exception ex) {
                failures.add(url + " -> " + ex.getMessage());
                log.debug("VIOP_SETTLEMENT_SOURCE_FAIL url={} reason={}", url, ex.toString());
            }
            sleepQuietly();
        }
        if (out.isEmpty()) {
            throw new IOException("No settlement rows parsed from any source. attempts=" + failures);
        }
        return dedupeSettlements(out);
    }

    private int writeAliasHistory(List<ViopAliasSnapshot> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            return 0;
        }
        List<MarketPriceUpdatedEvent> events = new ArrayList<>();
        for (ViopAliasSnapshot alias : aliases) {
            Long instrumentId =
                    instrumentMappingService
                            .resolveInstrument(SOURCE, alias.aliasSymbol().toUpperCase(Locale.ROOT))
                            .orElse(null);
            Instant observedAt = alias.tradeDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            events.add(
                    MarketPriceUpdatedEvent.ofAt(
                            alias.aliasSymbol().toUpperCase(Locale.ROOT),
                            alias.value(),
                            PRICE_TYPE,
                            SOURCE,
                            instrumentId,
                            observedAt));
        }
        marketHistoryWriteService.saveBatch(events);
        return events.size();
    }

    private List<ViopContractRow> dedupeContracts(List<ViopContractRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        java.util.LinkedHashMap<String, ViopContractRow> map = new java.util.LinkedHashMap<>();
        for (ViopContractRow row : rows) {
            if (row.contractCode() == null || row.contractCode().isBlank()) {
                continue;
            }
            map.put(row.contractCode().toUpperCase(Locale.ROOT), row);
        }
        return List.copyOf(map.values());
    }

    private List<ViopSettlementRow> dedupeSettlements(List<ViopSettlementRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        java.util.LinkedHashMap<String, ViopSettlementRow> map = new java.util.LinkedHashMap<>();
        for (ViopSettlementRow row : rows) {
            if (row.contractCode() == null || row.contractCode().isBlank() || row.tradeDate() == null) {
                continue;
            }
            String key = row.tradeDate() + "|" + row.contractCode().toUpperCase(Locale.ROOT);
            map.put(key, row);
        }
        return List.copyOf(map.values());
    }

    private void sleepQuietly() {
        long wait = Math.max(0L, properties.getRequestSpacingMs());
        if (wait == 0L) {
            return;
        }
        try {
            Thread.sleep(wait);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private List<String> resolveContractUrls() {
        if (!properties.getContractsUrls().isEmpty()) {
            return List.copyOf(properties.getContractsUrls());
        }
        return generateUrlsFromTemplate(properties.getContractsPathTemplate(), LocalDate.now(resolveZone()));
    }

    private List<String> resolveSettlementUrls(LocalDate fallbackTradeDate) {
        if (!properties.getSettlementUrls().isEmpty()) {
            return List.copyOf(properties.getSettlementUrls());
        }
        return generateUrlsFromTemplate(properties.getSettlementPathTemplate(), fallbackTradeDate);
    }

    private List<String> generateUrlsFromTemplate(String pathTemplate, LocalDate baseDate) {
        if (pathTemplate == null || pathTemplate.isBlank()) {
            return List.of();
        }
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return List.of();
        }
        int days = Math.max(1, properties.getSourceLookupDays());
        String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String template = pathTemplate.startsWith("/") ? pathTemplate : "/" + pathTemplate;
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = baseDate.minusDays(i);
            String ymd = d.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            urls.add(cleanBase + template.replace("{yyyyMMdd}", ymd).replace("YYYYAAGG", ymd));
        }
        return urls;
    }

    private ZoneId resolveZone() {
        try {
            return ZoneId.of(properties.getZone());
        } catch (Exception ignored) {
            return ZoneOffset.UTC;
        }
    }
}

