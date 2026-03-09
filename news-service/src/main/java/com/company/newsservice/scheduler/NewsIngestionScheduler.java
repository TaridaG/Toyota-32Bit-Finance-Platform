package com.company.newsservice.scheduler;

import com.company.newsservice.config.NewsProperties;
import com.company.newsservice.service.NewsIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsIngestionScheduler {

    private final NewsProperties newsProperties;
    private final NewsIngestionService newsIngestionService;

    @Scheduled(fixedDelayString = "${news.scheduler.delay-ms:300000}")
    public void ingest() {
        if (!newsProperties.getScheduler().isEnabled()) {
            log.debug("News ingestion scheduler disabled.");
            return;
        }

        int created = newsIngestionService.ingestLatest();
        log.info("News scheduler run completed. created={}", created);
    }
}