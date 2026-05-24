package com.company.marketdataservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.company.marketdataservice.bootstrap.config.DovizBankRatesProperties;
import com.company.marketdataservice.bootstrap.config.FundMarketProperties;
import com.company.marketdataservice.bootstrap.config.FxMarketProperties;
import com.company.marketdataservice.bootstrap.config.MarketDataProperties;

/**
 * Market Data Service Spring Boot giriş noktası; scheduler ve yapılandırma property bean'lerini etkinleştirir.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        MarketDataProperties.class,
        FxMarketProperties.class,
        FundMarketProperties.class,
        DovizBankRatesProperties.class
})
public class MarketDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketDataServiceApplication.class, args);
    }
}
