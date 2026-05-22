package com.company.newsservice.image;

import com.company.newsservice.config.NewsProperties;
import com.company.newsservice.domain.NewsArticle;
import com.company.newsservice.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Optional image enrichment: RSS description image first, then Open Graph fetch from {@code articleUrl}.
 * Does not interact with {@link com.company.newsservice.service.translation.NewsTranslationService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsArticleImageService {

    private final NewsProperties newsProperties;
    private final NewsArticleRepository newsArticleRepository;
    private final RssDescriptionImageExtractor rssDescriptionImageExtractor;
    private final ArticlePageImageFetcher articlePageImageFetcher;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void scheduleResolveAfterIngest(Long articleId, String rssSummaryHint) {
        NewsProperties.Image cfg = newsProperties.getImage();
        if (cfg == null || !cfg.isEnabled() || articleId == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new NewsArticleImageResolveEvent(articleId, rssSummaryHint));
    }

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
