package com.company.newsservice.image.infrastructure.scheduler;

import com.company.newsservice.bootstrap.config.NewsProperties;
import com.company.newsservice.image.application.ResolveArticleImageUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Eksik article image URL'lerini periyodik batch job ile backfill eder.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsArticleImageBackfillScheduler {

    private final NewsProperties newsProperties;
    private final ResolveArticleImageUseCase newsArticleImageService;

    /** Yapılandırılmış batch size ile backfill döngüsünü çalıştırır. */
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
