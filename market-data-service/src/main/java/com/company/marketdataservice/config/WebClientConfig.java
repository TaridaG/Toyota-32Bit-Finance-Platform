package com.company.marketdataservice.config;

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

@Configuration
public class WebClientConfig {

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
}