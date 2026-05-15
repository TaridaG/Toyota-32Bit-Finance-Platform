package com.company.finance_api.market.eurobond.yahoo;

import com.company.finance_api.config.MarketTrUsdEurobondYahooProperties;
import java.net.URI;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class YahooEurobondChartClient {

    private final RestClient restClient;
    private final MarketTrUsdEurobondYahooProperties properties;

    public YahooEurobondChartClient(
            RestClient.Builder restClientBuilder, MarketTrUsdEurobondYahooProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    public YahooEurobondChartResponse fetchHistoricalChart(String symbol, String range, String interval) {
        String normalized = normalizeSymbol(symbol);
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        URI uri = chartUri(base, normalized, range, interval);
        return restClient
                .get()
                .uri(uri)
                .header(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .retrieve()
                .body(YahooEurobondChartResponse.class);
    }

    private URI chartUri(String baseUrl, String normalizedSymbol, String range, String interval) {
        String tpl = properties.getChartPathTemplate().trim();
        if (tpl.isEmpty()) {
            tpl = "/v8/finance/chart/{symbol}";
        }
        if (!tpl.startsWith("/")) {
            tpl = "/" + tpl;
        }
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(baseUrl);
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
