package com.company.notification.bootstrap.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * finance-api HTTP API bağlantı ayarları ({@code services.finance-api.base-url}).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "services.finance-api")
public class FinanceApiProperties {

    private String baseUrl = "http://localhost:8080";
}
