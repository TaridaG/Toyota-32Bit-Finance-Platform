package com.company.notification.bootstrap.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Outbound adapter'ların kullandığı HTTP client'ları kaydeder (örn. instrument lookup).
 */
@Configuration
@EnableConfigurationProperties(FinanceApiProperties.class)
public class HttpClientConfig {

    /** finance-api çağrıları için paylaşılan {@link RestTemplate}. */
    @Bean
    public RestTemplate notificationRestTemplate() {
        return new RestTemplate();
    }
}
