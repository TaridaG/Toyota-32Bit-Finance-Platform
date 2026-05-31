package com.company.marketdataservice.spot.infrastructure.provider.coingecko;
import com.company.marketdataservice.catalog.registry.providers.CryptoRegistry;
import com.company.marketdataservice.history.domain.HistoricalPricePoint;
import com.company.marketdataservice.history.domain.HistoricalPriceProvider;
import com.company.marketdataservice.catalog.application.InstrumentMappingService;
import com.company.marketdataservice.catalog.infrastructure.persistence.ProviderInstrumentMapping;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
/**
 * `spot fiyat` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
@Component
public class CoinGeckoHistoricalPriceProvider implements HistoricalPriceProvider {

    private static final String SOURCE = "COINGECKO";
    private static final String PRICE_TYPE = "MARKET";
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_WAIT_MS = 750L;
    private static final Logger log = LoggerFactory.getLogger(CoinGeckoHistoricalPriceProvider.class);
    private static final String COINGECKO_PROVIDER = "COINGECKO";
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final InstrumentMappingService instrumentMappingService;

    @Value("${providers.coingecko.base-url:https://api.coingecko.com/api/v3}")
    private String baseUrl;

    @Value("${providers.coingecko.api-key:}")
    private String apiKey;

    public CoinGeckoHistoricalPriceProvider(
            WebClient webClient,
            ObjectMapper objectMapper,
            InstrumentMappingService instrumentMappingService
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.instrumentMappingService = instrumentMappingService;
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @param symbol enstrüman sembolü
         * @param startDate girdi parametresi
         * @param endDate girdi parametresi
         * @return işlem sonucu
         */
    @Override
    public List<HistoricalPricePoint> fetchRange(String symbol, LocalDate startDate, LocalDate endDate) {
        if (symbol == null || symbol.isBlank() || startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return List.of();
        }

        String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
        String coinId = resolveCoinId(normalizedSymbol);
        long from = startDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long to = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond() - 1;

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/coins/" + coinId + "/market_chart/range")
                .queryParam("vs_currency", "usd")
                .queryParam("from", from)
                .queryParam("to", to);
        if (apiKey != null && !apiKey.isBlank()) {
            uriBuilder.queryParam("x_cg_demo_api_key", apiKey.trim());
        }
        String uri = uriBuilder.toUriString();

        String body = getWithRetry(uri, coinId, normalizedSymbol);
        if (body == null || body.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode prices = root.path("prices");
            if (!prices.isArray() || prices.isEmpty()) {
                return List.of();
            }

            List<HistoricalPricePoint> out = new ArrayList<>();
            for (JsonNode pair : prices) {
                if (!pair.isArray() || pair.size() < 2) {
                    continue;
                }
                JsonNode tsNode = pair.get(0);
                JsonNode priceNode = pair.get(1);
                if (tsNode == null || !tsNode.canConvertToLong() || priceNode == null || !priceNode.isNumber()) {
                    continue;
                }

                Instant observedAt = Instant.ofEpochMilli(tsNode.longValue());
                BigDecimal price = priceNode.decimalValue();
                out.add(new HistoricalPricePoint(
                        normalizedSymbol,
                        price,
                        PRICE_TYPE,
                        SOURCE,
                        observedAt,
                        null
                ));
            }

            out.sort(Comparator.comparing(HistoricalPricePoint::occurredAt));
            return out;
        } catch (Exception ex) {
            log.warn(
                    "COINGECKO_HISTORICAL_PARSE_FAILED symbol={} coinId={} start={} end={} reason={}",
                    normalizedSymbol,
                    coinId,
                    startDate,
                    endDate,
                    ex.getMessage()
            );
            return List.of();
        }
    }

    private String getWithRetry(String uri, String coinId, String symbol) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return webClient.get()
                        .uri(uri)
                        .headers(h -> {
                            if (apiKey != null && !apiKey.isBlank()) {
                                String key = apiKey.trim();
                                h.set("x-cg-demo-api-key", key);
                                h.set("x-cg-pro-api-key", key);
                            }
                        })
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            } catch (WebClientResponseException ex) {
                boolean rateLimited = ex.getStatusCode().value() == 429;
                if (rateLimited && attempt < MAX_ATTEMPTS) {
                    sleepRetry();
                    continue;
                }
                log.warn(
                        "COINGECKO_HISTORICAL_FETCH_FAILED symbol={} coinId={} status={} attempt={} reason={}",
                        symbol,
                        coinId,
                        ex.getStatusCode().value(),
                        attempt,
                        ex.getMessage()
                );
                return null;
            } catch (Exception ex) {
                if (attempt < MAX_ATTEMPTS) {
                    sleepRetry();
                    continue;
                }
                log.warn(
                        "COINGECKO_HISTORICAL_FETCH_FAILED symbol={} coinId={} attempt={} reason={}",
                        symbol,
                        coinId,
                        attempt,
                        ex.getMessage()
                );
                return null;
            }
        }
        return null;
    }

    private static void sleepRetry() {
        try {
            Thread.sleep(RETRY_WAIT_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static String toCoinId(String symbol) {
        String upper = symbol.toUpperCase(Locale.ROOT);
        if (upper.endsWith("USDT")) {
            upper = upper.substring(0, upper.length() - 4);
        } else if (upper.endsWith("USD")) {
            upper = upper.substring(0, upper.length() - 3);
        } else if (upper.endsWith("TRY")) {
            upper = upper.substring(0, upper.length() - 3);
        }
        return upper.toLowerCase(Locale.ROOT);
    }

    private String resolveCoinId(String normalizedSymbol) {
        List<ProviderInstrumentMapping> mappings = instrumentMappingService
                .resolveInstrument(COINGECKO_PROVIDER, normalizedSymbol)
                .map(instrumentMappingService::getMappingsForInstrument)
                .orElse(List.of());

        return mappings.stream()
                .filter(this::isCoinGeckoMapping)
                .map(ProviderInstrumentMapping::getProviderSymbol)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .filter(s -> !looksLikePairSymbol(s))
                .findFirst()
                .map(String::toLowerCase)
                .orElseGet(() -> {
                    String baseAsset = extractBaseAsset(normalizedSymbol);
                    String mapped = CryptoRegistry.coingeckoIdForBase(baseAsset);
                    if (mapped != null) {
                        return mapped;
                    }
                    log.debug("COINGECKO_MAPPING_MISS symbol={} provider={} fallback=heuristic", normalizedSymbol, COINGECKO_PROVIDER);
                    return toCoinId(baseAsset);
                });
    }

    private boolean isCoinGeckoMapping(ProviderInstrumentMapping m) {
        return m != null
                && m.getProvider() != null
                && COINGECKO_PROVIDER.equalsIgnoreCase(m.getProvider());
    }

    private static boolean looksLikePairSymbol(String providerSymbol) {
        String upper = providerSymbol.toUpperCase(Locale.ROOT);
        return upper.endsWith("USDT") || upper.endsWith("USD") || upper.endsWith("TRY");
    }

    private static String extractBaseAsset(String symbol) {
        String upper = symbol.toUpperCase(Locale.ROOT);
        if (upper.endsWith("USDT")) {
            return upper.substring(0, upper.length() - 4);
        }
        if (upper.endsWith("USD") || upper.endsWith("TRY")) {
            return upper.substring(0, upper.length() - 3);
        }
        return upper;
    }
}
