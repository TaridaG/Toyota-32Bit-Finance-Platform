package com.company.finance_api.bootstrap.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Admin market asset yeniden hesaplama işleri için tek thread'li async executor config'i. */
@Configuration
@EnableAsync
public class AdminAsyncConfig {

  /** Admin market asset recompute görevleri için daemon tek thread executor bean'i. */
  @Bean(name = "adminMarketAssetTaskExecutor")
  public Executor adminMarketAssetTaskExecutor() {
    return Executors.newSingleThreadExecutor(
        r -> {
          Thread t = new Thread(r, "admin-market-asset-recompute");
          t.setDaemon(true);
          return t;
        });
  }
}
