package com.company.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(FinanceApiProperties.class)
public class HttpClientConfig {

    @Bean
    public RestTemplate notificationRestTemplate() {
        return new RestTemplate();
    }
}
