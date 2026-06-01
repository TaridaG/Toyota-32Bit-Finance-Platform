package com.company.marketdataservice.bootstrap.config;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Harici API çağrıları için timeout'lu {@link WebClient} bean tanımları (Netty tabanlı).
 */
@Configuration
public class WebClientConfig {

    /** Varsayılan spot provider ({@link MarketDataProperties#getProvider()}) için timeout'lu client. */
    @Bean
    public WebClient webClient(ResilienceProperties resilienceProperties, MarketDataProperties marketDataProperties) {

        String provider = marketDataProperties.getProvider();
        if ("composite".equalsIgnoreCase(provider)) {
            provider = "binance"; // composite ise default bir provider seçicem kontrol sonra
        }

        long timeoutMillis = resilienceProperties
                .getRequiredProvider(provider)
                .getTimeout()
                .getMillis();

        HttpClient httpClient =
                HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeoutMillis)
                        .responseTimeout(Duration.ofMillis(timeoutMillis))
                        .doOnConnected(conn ->
                                conn.addHandlerLast(
                                                new ReadTimeoutHandler(timeoutMillis, TimeUnit.MILLISECONDS)
                                        )
                                        .addHandlerLast(
                                                new WriteTimeoutHandler(timeoutMillis, TimeUnit.MILLISECONDS)
                                        )
                        );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /** TCMB / ExchangeRate FX kaynakları için 25 sn timeout'lu client. */
    @Bean("fxWebClient")
    public WebClient fxWebClient() {
        long timeoutMillis = 25_000L;
        HttpClient httpClient =
                HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeoutMillis)
                        .responseTimeout(Duration.ofMillis(timeoutMillis))
                        .doOnConnected(conn ->
                                conn.addHandlerLast(
                                                new ReadTimeoutHandler(timeoutMillis, TimeUnit.MILLISECONDS)
                                        )
                                        .addHandlerLast(
                                                new WriteTimeoutHandler(timeoutMillis, TimeUnit.MILLISECONDS)
                                        )
                        );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /** Yahoo Finance chart/quote istekleri için {@code resilience.providers.yahoo} timeout'u. */
    @Bean("yahooStockWebClient")
    public WebClient yahooStockWebClient(ResilienceProperties resilienceProperties) {
        long timeoutMillis = resilienceProperties.getRequiredProvider("yahoo").getTimeout().getMillis();
        HttpClient httpClient =
                HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeoutMillis)
                        .responseTimeout(Duration.ofMillis(timeoutMillis))
                        .doOnConnected(conn ->
                                conn.addHandlerLast(
                                                new ReadTimeoutHandler(timeoutMillis, TimeUnit.MILLISECONDS)
                                        )
                                        .addHandlerLast(
                                                new WriteTimeoutHandler(timeoutMillis, TimeUnit.MILLISECONDS)
                                        )
                        );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /** Finnhub REST istekleri için {@code resilience.providers.finnhub} timeout'u. */
    @Bean("finnhubWebClient")
    public WebClient finnhubWebClient(ResilienceProperties resilienceProperties) {
        long timeoutMillis = resilienceProperties.getRequiredProvider("finnhub").getTimeout().getMillis();
        HttpClient httpClient =
                HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeoutMillis)
                        .responseTimeout(Duration.ofMillis(timeoutMillis))
                        .doOnConnected(conn ->
                                conn.addHandlerLast(
                                                new ReadTimeoutHandler(timeoutMillis, TimeUnit.MILLISECONDS)
                                        )
                                        .addHandlerLast(
                                                new WriteTimeoutHandler(timeoutMillis, TimeUnit.MILLISECONDS)
                                        )
                        );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /** TEFAS.gov.tr fon API çağrıları için 20 sn timeout'lu client. */
    @Bean("tefasWebClient")
    public WebClient tefasWebClient() {
        long timeoutMillis = 20_000L;
        HttpClient httpClient =
                HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeoutMillis)
                        .responseTimeout(Duration.ofMillis(timeoutMillis))
                        .doOnConnected(conn ->
                                conn.addHandlerLast(
                                                new ReadTimeoutHandler(timeoutMillis, TimeUnit.MILLISECONDS)
                                        )
                                        .addHandlerLast(
                                                new WriteTimeoutHandler(timeoutMillis, TimeUnit.MILLISECONDS)
                                        )
                        );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}