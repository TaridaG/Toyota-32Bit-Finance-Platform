package com.company.reporting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ReportingClientConfig {

    @Bean
    public RestClient analyticsRestClient() {
        return RestClient.builder().build();
    }
}