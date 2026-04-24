package com.company.marketdataservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "market.fund")
public class FundMarketProperties {

    private boolean schedulerEnabled;

    private long delayMs = 3_600_000L;

    private List<String> trackedFundCodes = new ArrayList<>();

    private String httpUrlTemplate = "";
}
