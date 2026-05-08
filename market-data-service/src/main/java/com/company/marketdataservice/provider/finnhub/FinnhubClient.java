package com.company.marketdataservice.provider.finnhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.company.marketdataservice.config.FinnhubProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.Locale;

@Component
public class FinnhubClient {

    private final WebClient finnhubWebClient;
    private final FinnhubProperties properties;

    public FinnhubClient(
            @Qualifier("finnhubWebClient") WebClient finnhubWebClient,
            FinnhubProperties properties
    ) {
        this.finnhubWebClient = finnhubWebClient;
        this.properties = properties;
    }

    public FinnhubCandleResponse fetchStockCandles(String symbol, String resolution, long fromEpochSec, long toEpochSec) {
        String apiKey = requireApiKey();
        String normalized = normalizeSymbol(symbol);
        String encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8);
        String uri = buildPath(properties.getStockCandlePath())
                + "?symbol=" + encoded
                + "&resolution=" + resolution
                + "&from=" + fromEpochSec
                + "&to=" + toEpochSec
                + "&token=" + apiKey;
        try {
            return finnhubWebClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(FinnhubCandleResponse.class)
                    .block();
        } catch (WebClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.is4xxClientError()) {
                throw new IllegalArgumentException(
                        "Finnhub rejected symbol=" + normalized + " status=" + status.value(),
                        ex
                );
            }
            throw new IllegalStateException(
                    "Finnhub request failed symbol=" + normalized + " status=" + status.value(),
                    ex
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Finnhub request failed symbol=" + normalized, ex);
        }
    }

    public BigDecimal fetchLiveQuotePrice(String symbol) {
        String apiKey = requireApiKey();
        String normalized = normalizeSymbol(symbol);
        String encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8);
        String uri = buildPath(properties.getStockQuotePath()) + "?symbol=" + encoded + "&token=" + apiKey;
        try {
            FinnhubQuoteResponse response = finnhubWebClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(FinnhubQuoteResponse.class)
                    .block();
            if (response == null || response.c() == null || !Double.isFinite(response.c()) || response.c() <= 0.0d) {
                throw new IllegalStateException("Finnhub quote did not return a valid price for symbol=" + normalized);
            }
            return BigDecimal.valueOf(response.c());
        } catch (WebClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.is4xxClientError()) {
                throw new IllegalArgumentException(
                        "Finnhub rejected symbol=" + normalized + " status=" + status.value(),
                        ex
                );
            }
            throw new IllegalStateException(
                    "Finnhub request failed symbol=" + normalized + " status=" + status.value(),
                    ex
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Finnhub request failed symbol=" + normalized, ex);
        }
    }

    public JsonNode fetchCompanyProfile(String symbol) {
        String normalized = normalizeSymbol(symbol);
        String uri = buildPath(properties.getStockProfilePath())
                + "?symbol=" + encode(normalized)
                + "&token=" + requireApiKey();
        return fetchJson(uri, normalized);
    }

    public JsonNode fetchFinancialsReported(String symbol) {
        String normalized = normalizeSymbol(symbol);
        String uri = buildPath(properties.getStockFinancialsReportedPath())
                + "?symbol=" + encode(normalized)
                + "&token=" + requireApiKey();
        return fetchJson(uri, normalized);
    }

    public JsonNode fetchBasicFinancials(String symbol) {
        String normalized = normalizeSymbol(symbol);
        String uri = buildPath(properties.getStockMetricPath())
                + "?symbol=" + encode(normalized)
                + "&metric=all"
                + "&token=" + requireApiKey();
        return fetchJson(uri, normalized);
    }

    private JsonNode fetchJson(String uri, String normalizedSymbol) {
        try {
            return finnhubWebClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.is4xxClientError()) {
                throw new IllegalArgumentException(
                        "Finnhub rejected symbol=" + normalizedSymbol + " status=" + status.value(),
                        ex
                );
            }
            throw new IllegalStateException(
                    "Finnhub request failed symbol=" + normalizedSymbol + " status=" + status.value(),
                    ex
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Finnhub request failed symbol=" + normalizedSymbol, ex);
        }
    }

    private String requireApiKey() {
        String apiKey = properties.getApiKey() == null ? "" : properties.getApiKey().trim();
        if (apiKey.isEmpty()) {
            throw new IllegalStateException("Finnhub API key is not configured");
        }
        return apiKey;
    }

    private String buildPath(String path) {
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return base + normalizedPath;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is blank");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
