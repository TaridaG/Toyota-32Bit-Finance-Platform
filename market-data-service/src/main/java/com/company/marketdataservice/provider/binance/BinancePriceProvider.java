package com.company.marketdataservice.provider.binance;

import com.company.marketdataservice.provider.CircuitBreaker;
import com.company.marketdataservice.provider.PriceProvider;
import com.company.marketdataservice.provider.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;

@Component("binance")
@RequiredArgsConstructor
public class BinancePriceProvider implements PriceProvider {

    private final WebClient webClient;
    private final RateLimiter rateLimiter = new RateLimiter(5);
    private final CircuitBreaker circuitBreaker =
            new CircuitBreaker(3, Duration.ofSeconds(30));

    @Value("${providers.binance.base-url:https://api.binance.com}")
    private String baseUrl;

    @Override
    public String source() {
        return "BINANCE";
    }

    @Override
    public BigDecimal fetchPrice(String symbol) {
        circuitBreaker.beforeCall();
        rateLimiter.acquire();

        try {
            BinanceTickerResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v3/ticker/price")
                            .queryParam("symbol", symbol)
                            .build())
                    .retrieve()
                    .bodyToMono(BinanceTickerResponse.class)
                    .block();

            if (response == null || response.price() == null) {
                throw new IllegalStateException("Binance price is null for symbol=" + symbol);
            }

            BigDecimal price = new BigDecimal(response.price());
            circuitBreaker.recordSuccess();
            return price;

        } catch (Exception e) {
            circuitBreaker.recordFailure();
            throw e;
        }
    }

    record BinanceTickerResponse(String symbol, String price) {}
}
