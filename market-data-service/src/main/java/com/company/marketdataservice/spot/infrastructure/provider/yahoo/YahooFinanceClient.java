package com.company.marketdataservice.spot.infrastructure.provider.yahoo;
import com.fasterxml.jackson.databind.JsonNode;
import com.company.marketdataservice.bootstrap.config.YahooFinanceProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Locale;

/**
 * `spot fiyat` harici HTTP/API client adaptörü.
 */
@Component
public class YahooFinanceClient {

    private final WebClient yahooStockWebClient;
    private final YahooFinanceProperties properties;

    public YahooFinanceClient(
            @Qualifier("yahooStockWebClient") WebClient yahooStockWebClient,
            YahooFinanceProperties properties
    ) {
        this.yahooStockWebClient = yahooStockWebClient;
        this.properties = properties;
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @param symbol enstrüman sembolü
         * @return işlem sonucu
         */
    public YahooFinanceResponse fetchSpotChart(String symbol) {
        return fetchChart(symbol, properties.getDefaultRange(), properties.getDefaultInterval());
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @param symbol enstrüman sembolü
         * @param range girdi parametresi
         * @param interval girdi parametresi
         * @return işlem sonucu
         */
    public YahooFinanceResponse fetchHistoricalChart(String symbol, String range, String interval) {
        return fetchChart(symbol, range, interval);
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @param symbol enstrüman sembolü
         * @return işlem sonucu
         */
    public BigDecimal fetchSharesOutstanding(String symbol) {
        String normalized = normalizeSymbol(symbol);
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        String uri = base + "/v10/finance/quoteSummary/" + normalized + "?modules=defaultKeyStatistics";
        try {
            JsonNode body = yahooStockWebClient.get()
                    .uri(uri)
                    .header(HttpHeaders.USER_AGENT, properties.getUserAgent())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            JsonNode valueNode = body
                    .path("quoteSummary")
                    .path("result")
                    .path(0)
                    .path("defaultKeyStatistics")
                    .path("sharesOutstanding")
                    .path("raw");
            if (valueNode.isMissingNode() || valueNode.isNull()) {
                throw new IllegalStateException("Yahoo sharesOutstanding missing for symbol=" + normalized);
            }
            return valueNode.decimalValue();
        } catch (WebClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.is4xxClientError()) {
                throw new IllegalArgumentException(
                        "Yahoo rejected shares symbol=" + normalized + " status=" + status.value(),
                        ex
                );
            }
            throw new IllegalStateException(
                    "Yahoo shares request failed symbol=" + normalized + " status=" + status.value(),
                    ex
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Yahoo shares request failed symbol=" + normalized, ex);
        }
    }

    private YahooFinanceResponse fetchChart(String symbol, String range, String interval) {
        String normalized = normalizeSymbol(symbol);
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        URI uri = chartUri(base, normalized, range, interval);

        try {
            return yahooStockWebClient.get()
                    .uri(uri)
                    .header(HttpHeaders.USER_AGENT, properties.getUserAgent())
                    .retrieve()
                    .bodyToMono(YahooFinanceResponse.class)
                    .block();
        } catch (WebClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.is4xxClientError()) {
                throw new IllegalArgumentException(
                        "Yahoo rejected symbol=" + normalized + " status=" + status.value(),
                        ex
                );
            }
            throw new IllegalStateException(
                    "Yahoo request failed symbol=" + normalized + " status=" + status.value(),
                    ex
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Yahoo request failed symbol=" + normalized, ex);
        }
    }

    /**
     * Builds a chart URI with each path segment encoded (required for symbols like {@code GTUSDTR5Y:GOV}).
     */
    private URI chartUri(String baseUrl, String normalizedSymbol, String range, String interval) {
        String tpl = properties.getChartPathTemplate().trim();
        if (tpl.isEmpty()) {
            tpl = "/v8/finance/chart/{symbol}";
        }
        if (!tpl.startsWith("/")) {
            tpl = "/" + tpl;
        }
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(baseUrl);
        for (String segment : tpl.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            if ("{symbol}".equals(segment)) {
                b.pathSegment(normalizedSymbol);
            } else {
                b.pathSegment(segment);
            }
        }
        return b.queryParam("range", range).queryParam("interval", interval).encode().build().toUri();
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is blank");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
