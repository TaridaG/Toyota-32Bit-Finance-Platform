package com.company.newsservice.image;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Runs image resolution after ingest transaction commits so translation latency is unchanged.
 */
@Component
@RequiredArgsConstructor
public class NewsArticleImageResolveListener {

    private final NewsArticleImageService newsArticleImageService;

    @Async("newsImageExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleIngested(NewsArticleImageResolveEvent event) {
        newsArticleImageService.resolveForArticle(event.articleId(), event.rssSummaryHint());
    }
}
