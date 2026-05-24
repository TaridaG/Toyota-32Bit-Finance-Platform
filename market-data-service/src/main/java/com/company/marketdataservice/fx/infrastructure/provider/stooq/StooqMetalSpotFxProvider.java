package com.company.marketdataservice.fx.infrastructure.provider.stooq;
import com.company.marketdataservice.fx.domain.FxSnapshot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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

    private static final BigDecimal HALF_SPREAD = new BigDecimal("0.00025");
    private static final DateTimeFormatter STOOQ_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Map<String, String> STOOQ_SPOT_TO_CANONICAL = Map.of(
            "XAUUSD", "XAUTRY",
            "XAGUSD", "XAGTRY",
            "XPTUSD", "XPTTRY",
            "XPDUSD", "XPDTRY"
    );

    private final WebClient fxWebClient;

    public StooqMetalSpotFxProvider(@Qualifier("fxWebClient") WebClient fxWebClient) {
        this.fxWebClient = fxWebClient;
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @return işlem sonucu
         */
    public List<FxSnapshot> fetchLatestRates() {
        Optional<BigDecimal> usdTryOpt = fetchYahooUsdTry();
        if (usdTryOpt.isEmpty()) {
            return List.of();
        }
        BigDecimal usdTry = usdTryOpt.get();
        List<FxSnapshot> out = new ArrayList<>();
        for (Map.Entry<String, String> e : STOOQ_SPOT_TO_CANONICAL.entrySet()) {
            Optional<SpotQuote> quoteOpt = fetchStooqQuote(e.getKey());
            if (quoteOpt.isEmpty()) {
                continue;
            }
            SpotQuote quote = quoteOpt.get();
            BigDecimal midTry = quote.priceUsd().multiply(usdTry).setScale(6, RoundingMode.HALF_UP);
            BigDecimal bid = midTry.subtract(midTry.multiply(HALF_SPREAD)).setScale(6, RoundingMode.HALF_UP);
            BigDecimal ask = midTry.add(midTry.multiply(HALF_SPREAD)).setScale(6, RoundingMode.HALF_UP);
            String base = e.getValue().replace("TRY", "");
            out.add(new FxSnapshot(
                    e.getValue(),
                    base,
                    "TRY",
                    bid,
                    ask,
                    midTry,
                    quote.timestamp(),
                    "STOOQ_SPOT"
            ));
        }
        return out;
    }

    private Optional<BigDecimal> fetchYahooUsdTry() {
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/USDTRY=X?range=1d&interval=1m";
        try {
            String body = fxWebClient.get().uri(url).retrieve().bodyToMono(String.class).block();
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            int idx = body.indexOf("\"regularMarketPrice\":");
            if (idx < 0) {
                return Optional.empty();
            }
            int start = idx + "\"regularMarketPrice\":".length();
            int end = start;
            while (end < body.length() && ("0123456789.-".indexOf(body.charAt(end)) >= 0)) {
                end++;
            }
            if (end <= start) {
                return Optional.empty();
            }
            return Optional.of(new BigDecimal(body.substring(start, end)));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<SpotQuote> fetchStooqQuote(String symbol) {
        String url = "https://stooq.com/q/l/?s=" + symbol.toLowerCase() + "&i=d";
        try {
            String csv = fxWebClient.get().uri(url).retrieve().bodyToMono(String.class).block();
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
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private record SpotQuote(BigDecimal priceUsd, Instant timestamp) {
    }
}
