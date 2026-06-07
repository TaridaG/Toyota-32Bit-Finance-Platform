package com.company.marketdataservice.fx.infrastructure.provider.exchangerate;
import com.company.marketdataservice.bootstrap.config.FxMarketProperties;
import com.company.marketdataservice.fx.domain.FxProvider;
import com.company.marketdataservice.fx.domain.FxSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * `FX spot` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
@Component
public class ExchangeRateApiFxProvider implements FxProvider {

    private static final String QUOTE = "TRY";
    private static final BigDecimal HALF_SPREAD = new BigDecimal("0.00025");

    private final FxMarketProperties fxMarketProperties;
    private final ObjectMapper objectMapper;
    private final WebClient fxWebClient;

    public ExchangeRateApiFxProvider(
            FxMarketProperties fxMarketProperties,
            ObjectMapper objectMapper,
            @Qualifier("fxWebClient") WebClient fxWebClient
    ) {
        this.fxMarketProperties = fxMarketProperties;
        this.objectMapper = objectMapper;
        this.fxWebClient = fxWebClient;
    }

    /**
     * Exchange Rate API tabanlı provider tanımlayıcısını ({@code EXCHANGE_RATE_API}) döner.
     */
    @Override
    public String source() {
        return "EXCHANGE_RATE_API";
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @return işlem sonucu
         */
    @Override
    public List<FxSnapshot> fetchLatestRates() {
        String url = fxMarketProperties.getExchangeRateUrl();
        String body = fxWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        if (body == null || body.isBlank()) {
            return List.of();
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception ex) {
            return List.of();
        }
        if (!root.path("result").asText("").equalsIgnoreCase("success")) {
            return List.of();
        }
        JsonNode rates = root.path("rates");
        if (rates.isMissingNode() || !rates.isObject()) {
            return List.of();
        }
        Instant ts = Instant.now();
        if (root.has("time_last_update_unix") && root.get("time_last_update_unix").canConvertToLong()) {
            long epoch = root.get("time_last_update_unix").asLong();
            if (epoch > 0) {
                ts = Instant.ofEpochSecond(epoch);
            }
        }
        List<FxSnapshot> out = new ArrayList<>();
        for (String ccyRaw : fxMarketProperties.getProviderCurrencies()) {
            if (ccyRaw == null || ccyRaw.isBlank()) {
                continue;
            }
            String ccy = ccyRaw.trim().toUpperCase(Locale.ROOT);
            if (QUOTE.equals(ccy)) {
                continue;
            }
            JsonNode rateNode = rates.get(ccy);
            if (rateNode == null || !rateNode.isNumber()) {
                continue;
            }
            BigDecimal foreignPerOneTry = rateNode.decimalValue();
            if (foreignPerOneTry.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal mid = BigDecimal.ONE.divide(foreignPerOneTry, 6, RoundingMode.HALF_UP);
            BigDecimal bid = mid.subtract(mid.multiply(HALF_SPREAD)).setScale(6, RoundingMode.HALF_UP);
            BigDecimal ask = mid.add(mid.multiply(HALF_SPREAD)).setScale(6, RoundingMode.HALF_UP);
            String canonical = ccy + QUOTE;
            out.add(new FxSnapshot(canonical, ccy, QUOTE, bid, ask, mid, ts, source()));
        }
        return out;
    }
}
