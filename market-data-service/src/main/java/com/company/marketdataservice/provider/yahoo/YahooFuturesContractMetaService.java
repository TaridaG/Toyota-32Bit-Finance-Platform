package com.company.marketdataservice.provider.yahoo;

import com.company.marketdataservice.config.MetalFuturesSymbols;
import com.company.marketdataservice.config.YahooFinanceProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class YahooFuturesContractMetaService {

    private static final long CACHE_TTL_MS = 3_600_000L;

    private final WebClient yahooStockWebClient;
    private final YahooFinanceProperties properties;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public YahooFuturesContractMetaService(
            @Qualifier("yahooStockWebClient") WebClient yahooStockWebClient,
            YahooFinanceProperties properties) {
        this.yahooStockWebClient = yahooStockWebClient;
        this.properties = properties;
    }

    public ContractMeta resolve(String symbol) {
        if (!MetalFuturesSymbols.isFutures(symbol)) {
            return ContractMeta.empty();
        }
        String key = symbol.trim().toUpperCase(Locale.ROOT);
        CacheEntry hit = cache.get(key);
        long now = System.currentTimeMillis();
        if (hit != null && hit.expiresAtMs > now) {
            return hit.meta;
        }
        ContractMeta fetched = fetchFromYahoo(key);
        cache.put(key, new CacheEntry(fetched, now + CACHE_TTL_MS));
        return fetched;
    }

    private ContractMeta fetchFromYahoo(String symbol) {
        String encoded = symbol.replace("=", "%3D");
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        String uri = base + "/v10/finance/quoteSummary/" + encoded
                + "?modules=price,summaryDetail,defaultKeyStatistics";
        try {
            JsonNode body = yahooStockWebClient.get()
                    .uri(uri)
                    .header(HttpHeaders.USER_AGENT, properties.getUserAgent())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (body == null || body.isMissingNode()) {
                return ContractMeta.empty();
            }
            JsonNode result = body.path("quoteSummary").path("result").path(0);
            String exchange = text(result, "price", "exchangeName");
            if (exchange == null) {
                exchange = text(result, "price", "fullExchangeName");
            }
            String underlying = text(result, "defaultKeyStatistics", "underlyingSymbol");
            Instant expiry = epochSeconds(result, "summaryDetail", "expireDate");
            if (expiry == null) {
                expiry = epochSeconds(result, "defaultKeyStatistics", "expireDate");
            }
            String shortName = text(result, "price", "shortName");
            return new ContractMeta(exchange, underlying, expiry, shortName);
        } catch (Exception ignored) {
            return ContractMeta.empty();
        }
    }

    private static String text(JsonNode root, String module, String field) {
        JsonNode node = root.path(module).path(field);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.has("raw")) {
            node = node.path("raw");
        }
        String s = node.asText(null);
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static Instant epochSeconds(JsonNode root, String module, String field) {
        JsonNode node = root.path(module).path(field);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.has("raw")) {
            node = node.path("raw");
        }
        long sec = node.asLong(0L);
        return sec > 0 ? Instant.ofEpochSecond(sec) : null;
    }

    public record ContractMeta(String exchangeName, String underlyingSymbol, Instant contractExpiry, String shortName) {
        public static ContractMeta empty() {
            return new ContractMeta(null, null, null, null);
        }
    }

    private record CacheEntry(ContractMeta meta, long expiresAtMs) {}
}
