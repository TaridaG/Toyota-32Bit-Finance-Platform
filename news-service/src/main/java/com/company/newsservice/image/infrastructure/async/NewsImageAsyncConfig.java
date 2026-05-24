package com.company.newsservice.image.infrastructure.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * News image async işlemleri için thread pool executor yapılandırması.
 */
@Configuration
@EnableAsync
public class NewsImageAsyncConfig {

    /** Image resolve listener'ın kullandığı {@code newsImageExecutor} bean'ini kaydeder. */
    @Bean(name = "newsImageExecutor")
    public Executor newsImageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("news-image-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(200);
        executor.initialize();
        return executor;
    }
}
