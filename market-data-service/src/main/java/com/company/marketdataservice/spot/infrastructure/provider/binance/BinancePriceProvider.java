package com.company.marketdataservice.spot.infrastructure.provider.binance;
import com.company.marketdataservice.spot.infrastructure.provider.PriceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

/**
 * `spot fiyat` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
@Component("binance")
@RequiredArgsConstructor
public class BinancePriceProvider implements PriceProvider {

    private final WebClient webClient;

    @Value("${providers.binance.base-url:https://api.binance.com}")
    private String baseUrl;

    /**
     * Bu provider'ın kaynak kimliğini döner.
         */
    @Override
    public String source() {
        return "BINANCE";
    }

    /**
     * Binance ticker REST API'sinden verilen sembol için spot fiyat fetch eder.
         * @param symbol Binance trading pair sembolü (ör. BTCUSDT)
         * @return parse edilmiş spot fiyat
         */
    @Override
    public BigDecimal fetchPrice(String symbol) {
        BinanceTickerResponse response = webClient.get()
                .uri(baseUrl + "/api/v3/ticker/price?symbol=" + symbol)
                .retrieve()
                .bodyToMono(BinanceTickerResponse.class)
                .block();

        if (response == null || response.price() == null) {
            throw new IllegalStateException("Binance price is null for symbol=" + symbol);
        }

        return new BigDecimal(response.price());
    }

    record BinanceTickerResponse(String symbol, String price) {
    }
}