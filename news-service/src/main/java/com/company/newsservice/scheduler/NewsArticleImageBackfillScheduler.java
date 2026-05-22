package com.company.newsservice.scheduler;

import com.company.newsservice.config.NewsProperties;
import com.company.newsservice.image.NewsArticleImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsArticleImageBackfillScheduler {

    private final NewsProperties newsProperties;
    private final NewsArticleImageService newsArticleImageService;

    @Scheduled(fixedDelayString = "${news.image.backfill-delay-ms:120000}")
    public void runBackfill() {
        NewsProperties.Image cfg = newsProperties.getImage();
        if (cfg == null || !cfg.isEnabled() || !cfg.isBackfillEnabled()) {
            return;
        }
        int resolved = newsArticleImageService.backfillMissingImages(cfg.getBackfillBatchSize());
        if (resolved > 0) {
            log.info("NEWS_IMAGE_BACKFILL resolved={}", resolved);
        }
    }
}
