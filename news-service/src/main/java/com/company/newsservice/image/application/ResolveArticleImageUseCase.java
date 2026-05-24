package com.company.newsservice.image.application;

import com.company.newsservice.bootstrap.config.NewsProperties;
import com.company.newsservice.query.domain.NewsArticle;
import com.company.newsservice.query.infrastructure.persistence.NewsArticleRepository;
import com.company.newsservice.image.infrastructure.http.ArticlePageImageFetcher;
import com.company.newsservice.image.infrastructure.async.NewsArticleImageResolveEvent;
import com.company.newsservice.image.infrastructure.rss.RssDescriptionImageExtractor;
import com.company.newsservice.image.infrastructure.normalizer.ArticleImageUrlNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Opsiyonel image enrichment use case: önce RSS description image, ardından {@code articleUrl} üzerinden Open Graph fetch.
 * {@link com.company.newsservice.translation.application.TranslateNewsUseCase} ile etkileşime girmez.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResolveArticleImageUseCase {

    private final NewsProperties newsProperties;
    private final NewsArticleRepository newsArticleRepository;
    private final RssDescriptionImageExtractor rssDescriptionImageExtractor;
    private final ArticlePageImageFetcher articlePageImageFetcher;
    private final ApplicationEventPublisher applicationEventPublisher;

    /** Ingest sonrası image resolve işlemini async application event ile planlar. */
    public void scheduleResolveAfterIngest(Long articleId, String rssSummaryHint) {
        NewsProperties.Image cfg = newsProperties.getImage();
        if (cfg == null || !cfg.isEnabled() || articleId == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new NewsArticleImageResolveEvent(articleId, rssSummaryHint));
    }

    /** Tek bir article için image URL çözer; bulunursa persist eder. */
    @Transactional
    public boolean resolveForArticle(Long articleId, String rssSummaryHint) {
        NewsProperties.Image cfg = newsProperties.getImage();
        if (cfg == null || !cfg.isEnabled() || articleId == null) {
            return false;
        }
        NewsArticle article = newsArticleRepository.findById(articleId).orElse(null);
        if (article == null || hasImage(article)) {
            return false;
        }

        String fromRss = rssDescriptionImageExtractor.extract(rssSummaryHint, article.getArticleUrl());
        if (fromRss != null) {
            persistImage(article, fromRss);
            log.debug("NEWS_IMAGE_RSS articleId={}", articleId);
            return true;
        }

        String fromPage = articlePageImageFetcher.fetch(article.getArticleUrl());
        if (fromPage != null) {
            persistImage(article, fromPage);
            log.debug("NEWS_IMAGE_PAGE articleId={}", articleId);
            return true;
        }
        return false;
    }

    /** Image URL'si eksik active article'ları batch halinde doldurur; çözülen satır sayısını döner. */
    @Transactional
    public int backfillMissingImages(int batchSize) {
        NewsProperties.Image cfg = newsProperties.getImage();
        if (cfg == null || !cfg.isEnabled() || !cfg.isBackfillEnabled() || batchSize <= 0) {
            return 0;
        }
        List<NewsArticle> batch = newsArticleRepository
                .findActiveWithoutImageUrl(PageRequest.of(0, batchSize))
                .getContent();
        int resolved = 0;
        for (NewsArticle article : batch) {
            if (resolveForArticle(article.getId(), article.getSummary())) {
                resolved++;
            }
        }
        return resolved;
    }

    private void persistImage(NewsArticle article, String imageUrl) {
        article.setImageUrl(ArticleImageUrlNormalizer.truncateStored(imageUrl));
        newsArticleRepository.save(article);
    }

    private static boolean hasImage(NewsArticle article) {
        return article.getImageUrl() != null && !article.getImageUrl().isBlank();
    }
}
