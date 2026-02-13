package com.company.marketdataservice.provider.coingecko;

import com.company.marketdataservice.provider.PriceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CoinGeckoPriceProvider implements PriceProvider {

    private final WebClient webClient;

    @Value("${providers.coingecko.base-url:https://api.coingecko.com/api/v3}")
    private String baseUrl;

    @Override
    public BigDecimal fetchPrice(String symbol) {

        // BTCUSDT → btc
        String id = symbol.replace("USDT", "").toLowerCase();

        Map response = webClient.get()
                .uri(baseUrl + "/simple/price?ids=" + id + "&vs_currencies=usd")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map priceMap = (Map) response.get(id);

        return new BigDecimal(priceMap.get("usd").toString());
    }

    @Override
    public String source() {
        return "COINGECKO";
    }
}
