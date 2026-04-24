package com.company.marketdataservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.company.marketdataservice.config.FundMarketProperties;
import com.company.marketdataservice.config.FxMarketProperties;
import com.company.marketdataservice.config.MarketDataProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({MarketDataProperties.class, FxMarketProperties.class, FundMarketProperties.class})
public class MarketDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketDataServiceApplication.class, args);
    }
}
