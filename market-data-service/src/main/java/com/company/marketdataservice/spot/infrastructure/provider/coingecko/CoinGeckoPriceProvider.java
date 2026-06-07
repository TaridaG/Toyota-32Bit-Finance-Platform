package com.company.marketdataservice.spot.infrastructure.provider.coingecko;
import com.company.marketdataservice.spot.infrastructure.provider.PriceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

/**
 * `spot fiyat` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
@Component("coingecko")
@RequiredArgsConstructor
public class CoinGeckoPriceProvider implements PriceProvider {

    private final WebClient webClient;

    @Value("${providers.coingecko.base-url:https://api.coingecko.com/api/v3}")
    private String baseUrl;

    /**
     * CoinGecko simple/price REST API'sinden verilen sembol için USD spot fiyat fetch eder.
         * @param symbol enstrüman sembolü; USDT suffix'i CoinGecko coin id'sine dönüştürülür
         * @return USD cinsinden spot fiyat
         */
    @Override
    public BigDecimal fetchPrice(String symbol) {
        String id = symbol.replace("USDT", "").toLowerCase();

        Map<?, ?> response = webClient.get()
                .uri(baseUrl + "/simple/price?ids=" + id + "&vs_currencies=usd")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || response.get(id) == null) {
            throw new IllegalStateException("CoinGecko response is null for symbol=" + symbol);
        }

        Map<?, ?> priceMap = (Map<?, ?>) response.get(id);
        Object usd = priceMap.get("usd");

        if (usd == null) {
            throw new IllegalStateException("CoinGecko USD price is null for symbol=" + symbol);
        }

        return new BigDecimal(usd.toString());
    }

    /**
     * Bu provider'ın kaynak kimliğini döner.
         */
    @Override
    public String source() {
        return "COINGECKO";
    }
}