package com.company.newsservice.ingestion.infrastructure.scheduler;

import com.company.newsservice.bootstrap.config.NewsProperties;
import com.company.newsservice.ingestion.application.IngestLatestNewsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periyodik olarak {@link IngestLatestNewsUseCase} ile son haberleri ingestion pipeline'ına alır.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsIngestionScheduler {

    private final NewsProperties newsProperties;
    private final IngestLatestNewsUseCase ingestLatestNewsUseCase;

    /**
     * Scheduler ayarları aktifse haber ingestion use case'ini tetikler.
     */
    @Scheduled(fixedDelayString = "${news.scheduler.delay-ms:300000}")
    public void ingest() {
        if (!newsProperties.getScheduler().isEnabled()) {
            log.debug("News ingestion scheduler disabled.");
            return;
        }

        int created = ingestLatestNewsUseCase.ingestLatest();
        log.info("News scheduler run completed. created={}", created);
    }
}