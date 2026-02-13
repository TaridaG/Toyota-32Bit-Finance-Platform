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
    public WebClient webClient(ResilienceProperties resilienceProperties) {

        HttpClient httpClient =
                HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                                resilienceProperties.getTimeout().getSeconds() * 1000)
                        .responseTimeout(
                                Duration.ofSeconds(resilienceProperties.getTimeout().getSeconds())
                        )
                        .doOnConnected(conn ->
                                conn.addHandlerLast(
                                                new ReadTimeoutHandler(
                                                        resilienceProperties.getTimeout().getSeconds(),
                                                        TimeUnit.SECONDS
                                                )
                                        )
                                        .addHandlerLast(
                                                new WriteTimeoutHandler(
                                                        resilienceProperties.getTimeout().getSeconds(),
                                                        TimeUnit.SECONDS
                                                )
                                        )
                        );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
