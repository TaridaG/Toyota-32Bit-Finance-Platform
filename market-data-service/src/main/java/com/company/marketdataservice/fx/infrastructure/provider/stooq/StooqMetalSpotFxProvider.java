package com.company.marketdataservice.fx.infrastructure.provider.stooq;
import com.company.marketdataservice.fx.domain.FxSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * `FX spot` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
@Component
public class StooqMetalSpotFxProvider {

    private static final Logger log = LoggerFactory.getLogger(StooqMetalSpotFxProvider.class);
    private static final BigDecimal HALF_SPREAD = new BigDecimal("0.00025");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final DateTimeFormatter STOOQ_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String MINTED_METAL_URL = "https://mintedmetal.com/api/prices.json";
    private static final String STOOQ_URL_TEMPLATE = "https://stooq.com/q/l/?s=%s&i=d";
    private static final String REQUEST_USER_AGENT = "Mozilla/5.0 (compatible; FinanceMarketDataService/1.0)";
    private static final Map<String, String> MINTED_METAL_TO_CANONICAL = Map.of(
            "gold", "XAUTRY",
            "silver", "XAGTRY",
            "platinum", "XPTTRY",
            "palladium", "XPDTRY"
    );
    private static final Map<String, String> STOOQ_SPOT_TO_CANONICAL = Map.of(
            "XAUUSD", "XAUTRY",
            "XAGUSD", "XAGTRY",
            "XPTUSD", "XPTTRY",
            "XPDUSD", "XPDTRY",
            "XCUUSD", "XCUTRY"
    );

    private final WebClient fxWebClient;
    private final ObjectMapper objectMapper;
    private final String mintedMetalUrl;
    private final String stooqUrlTemplate;

    @Autowired
    public StooqMetalSpotFxProvider(
            @Qualifier("fxWebClient") WebClient fxWebClient,
            ObjectMapper objectMapper
    ) {
        this(fxWebClient, objectMapper, MINTED_METAL_URL, STOOQ_URL_TEMPLATE);
    }

    StooqMetalSpotFxProvider(
            WebClient fxWebClient,
            ObjectMapper objectMapper,
            String mintedMetalUrl,
            String stooqUrlTemplate
    ) {
        this.fxWebClient = fxWebClient;
        this.objectMapper = objectMapper;
        this.mintedMetalUrl = mintedMetalUrl;
        this.stooqUrlTemplate = stooqUrlTemplate;
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @return işlem sonucu
         */
    public List<FxSnapshot> fetchLatestRates(BigDecimal usdTry, Instant observedAtFallback) {
        if (usdTry == null || usdTry.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        List<FxSnapshot> mintedSnapshots = fetchMintedMetalRates(usdTry, observedAtFallback);
        if (!mintedSnapshots.isEmpty()) {
            List<FxSnapshot> out = new ArrayList<>(mintedSnapshots);
            fetchStooqQuote("XCUUSD")
                    .map(quote -> toSnapshot("XCUTRY", quote.priceUsd(), usdTry, quote.timestamp(), "STOOQ_SPOT"))
                    .ifPresent(out::add);
            return out;
        }
        return fetchLegacyStooqRates(usdTry);
    }

    private List<FxSnapshot> fetchMintedMetalRates(BigDecimal usdTry, Instant observedAtFallback) {
        try {
            String body = fxWebClient.get()
                    .uri(mintedMetalUrl)
                    .header(HttpHeaders.USER_AGENT, REQUEST_USER_AGENT)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(REQUEST_TIMEOUT);
            if (body == null || body.isBlank()) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode metals = root.path("metals");
            if (!metals.isObject()) {
                return List.of();
            }
            Instant updatedAt = parseInstant(root.path("updatedAt").asText(null)).orElse(observedAtFallback);
            List<FxSnapshot> out = new ArrayList<>(MINTED_METAL_TO_CANONICAL.size());
            for (Map.Entry<String, String> entry : MINTED_METAL_TO_CANONICAL.entrySet()) {
                JsonNode metal = metals.path(entry.getKey());
                if (!metal.isObject()) {
                    continue;
                }
                JsonNode priceNode = metal.path("price");
                if (!priceNode.isNumber()) {
                    continue;
                }
                BigDecimal usdPrice = priceNode.decimalValue();
                if (usdPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                Instant observedAt =
                        parseInstant(metal.path("fixedAt").asText(null)).orElse(updatedAt != null ? updatedAt : observedAtFallback);
                out.add(toSnapshot(entry.getValue(), usdPrice, usdTry, observedAt, "MINTED_METAL_LBMA"));
            }
            if (out.isEmpty()) {
                log.warn("MINTED_METAL_SPOT_EMPTY");
            }
            return out;
        } catch (Exception ex) {
            log.warn("MINTED_METAL_SPOT_FAILED reason={}", ex.getMessage());
            return List.of();
        }
    }

    private Optional<SpotQuote> fetchStooqQuote(String symbol) {
        String url = stooqUrlTemplate.formatted(symbol.toLowerCase());
        try {
            String csv = fxWebClient.get()
                    .uri(url)
                    .header(HttpHeaders.USER_AGENT, REQUEST_USER_AGENT)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(REQUEST_TIMEOUT);
            if (csv == null || csv.isBlank()) {
                return Optional.empty();
            }
            String[] parts = csv.trim().split(",");
            if (parts.length < 7) {
                return Optional.empty();
            }
            if ("N/D".equalsIgnoreCase(parts[6].trim())) {
                return Optional.empty();
            }
            BigDecimal close = new BigDecimal(parts[6].trim());
            LocalDate date = LocalDate.parse(parts[1].trim(), STOOQ_DATE);
            Instant ts = date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
            return Optional.of(new SpotQuote(close, ts));
        } catch (Exception ex) {
            log.debug("STOOQ_SPOT_FETCH_FAILED symbol={} reason={}", symbol, ex.getMessage());
            return Optional.empty();
        }
    }

    private List<FxSnapshot> fetchLegacyStooqRates(BigDecimal usdTry) {
        List<FxSnapshot> out = new ArrayList<>();
        for (Map.Entry<String, String> entry : STOOQ_SPOT_TO_CANONICAL.entrySet()) {
            Optional<SpotQuote> quoteOpt = fetchStooqQuote(entry.getKey());
            if (quoteOpt.isEmpty()) {
                continue;
            }
            SpotQuote quote = quoteOpt.get();
            out.add(toSnapshot(entry.getValue(), quote.priceUsd(), usdTry, quote.timestamp(), "STOOQ_SPOT"));
        }
        return out;
    }

    private static FxSnapshot toSnapshot(
            String canonical,
            BigDecimal usdPrice,
            BigDecimal usdTry,
            Instant observedAt,
            String source
    ) {
        BigDecimal midTry = usdPrice.multiply(usdTry).setScale(6, RoundingMode.HALF_UP);
        BigDecimal bid = midTry.subtract(midTry.multiply(HALF_SPREAD)).setScale(6, RoundingMode.HALF_UP);
        BigDecimal ask = midTry.add(midTry.multiply(HALF_SPREAD)).setScale(6, RoundingMode.HALF_UP);
        String base = canonical.replace("TRY", "");
        return new FxSnapshot(
                canonical,
                base,
                "TRY",
                bid,
                ask,
                midTry,
                observedAt,
                source
        );
    }

    private static Optional<Instant> parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(raw));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private record SpotQuote(BigDecimal priceUsd, Instant timestamp) {
    }
}
