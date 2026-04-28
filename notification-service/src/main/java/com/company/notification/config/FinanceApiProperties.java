package com.company.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "services.finance-api")
public class FinanceApiProperties {

    private String baseUrl = "http://localhost:8080";
}
