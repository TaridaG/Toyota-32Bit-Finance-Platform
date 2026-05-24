package com.company.finance_api;

import com.company.finance_api.bootstrap.config.MarketTrUsdEurobondYahooProperties;
import com.company.finance_api.bootstrap.config.ProfileAvatarProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Finance API Spring Boot giriş noktası; scheduler ve profil avatar / TR USD eurobond Yahoo config
 * bean'lerini etkinleştirir.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
  ProfileAvatarProperties.class,
  MarketTrUsdEurobondYahooProperties.class
})
public class FinanceApiApplication {

  /** Uygulamayı başlatır. */
  public static void main(String[] args) {
    SpringApplication.run(FinanceApiApplication.class, args);
  }
}
