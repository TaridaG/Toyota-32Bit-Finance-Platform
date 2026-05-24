package com.company.analytics.bootstrap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Harici servis çağrıları için RestClient bean yapılandırması. */
@Configuration
public class AnalyticsClientConfig {

    /** Finance API için RestClient bean'i oluşturur. */
    @Bean
    public RestClient financeRestClient() {
        return RestClient.builder().build();
    }
}
