package com.company.newsservice.service.translation;

import com.company.newsservice.config.NewsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsTranslationBackfillScheduler {

    private final NewsProperties newsProperties;
    private final NewsTranslationService newsTranslationService;

    @Scheduled(fixedDelayString = "${news.translation.backfill-delay-ms:60000}")
    public void runBackfill() {
        NewsProperties.Translation cfg = newsProperties.getTranslation();
        if (cfg == null || !cfg.isEnabled() || !cfg.isBackfillEnabled()) {
            return;
        }
        int created = newsTranslationService.backfillMissingTranslations(cfg.getBackfillBatchSize());
        if (created > 0) {
            log.info("NEWS_TRANSLATION_BACKFILL created={}", created);
        }
    }
}
