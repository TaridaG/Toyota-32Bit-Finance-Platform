package com.company.newsservice.image.infrastructure.async;

import com.company.newsservice.image.application.ResolveArticleImageUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Ingest transaction commit olduktan sonra image resolution çalıştırır; translation latency etkilenmez.
 */
@Component
@RequiredArgsConstructor
public class NewsArticleImageResolveListener {

    private static final Logger log = LoggerFactory.getLogger(NewsArticleImageResolveListener.class);

    private final ResolveArticleImageUseCase newsArticleImageService;

    /** {@link NewsArticleImageResolveEvent} alındığında {@link ResolveArticleImageUseCase} ile image çözer. */
    @Async("newsImageExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleIngested(NewsArticleImageResolveEvent event) {
        try {
            newsArticleImageService.resolveForArticle(event.articleId(), event.rssSummaryHint());
        } catch (Exception ex) {
            log.warn("NEWS_IMAGE_RESOLVE_FAILED articleId={}", event.articleId(), ex);
        }
    }
}
