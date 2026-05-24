package com.company.newsservice.ingestion.application;

import com.company.newsservice.query.domain.NewsArticle;
import com.company.newsservice.ingestion.domain.NewsProvider;
import com.company.newsservice.ingestion.domain.ProviderNewsItem;
import com.company.newsservice.query.infrastructure.persistence.NewsArticleRepository;
import com.company.newsservice.ingestion.infrastructure.matching.NewsInstrumentMatcher;
import com.company.newsservice.ingestion.infrastructure.relevance.NewsRelevanceEvaluator;
import com.company.newsservice.ingestion.infrastructure.matching.NewsTopicTagger;
import com.company.newsservice.image.infrastructure.normalizer.ArticleImageUrlNormalizer;
import com.company.newsservice.image.application.ResolveArticleImageUseCase;
import com.company.newsservice.translation.application.TranslateNewsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import com.company.newsservice.ingestion.infrastructure.kafka.NewsInstrumentMatchedEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Kayıtlı news provider'lardan son haberleri çeker, filtreler, enstrüman eşleşmesi uygular ve veritabanına kaydeder.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestLatestNewsUseCase {

    private final List<NewsProvider> newsProviders;
    private final NewsArticleRepository newsArticleRepository;
    private final NewsInstrumentMatcher newsInstrumentMatcher;
    private final NewsRelevanceEvaluator newsRelevanceEvaluator;
    private final NewsTopicTagger newsTopicTagger;
    private final TranslateNewsUseCase newsTranslationService;
    private final ResolveArticleImageUseCase newsArticleImageService;
    private final NewsInstrumentMatchedEventPublisher newsInstrumentMatchedEventPublisher;

    /**
     * Tüm provider'lardan haberleri çeker, relevance kontrolü ve enstrüman eşleşmesi uygular, yeni makaleleri persist eder.
     *
     * @return başarıyla kaydedilen makale sayısı
     */
    @Transactional
    public int ingestLatest() {
        int fetched = 0;
        int saved = 0;
        int skipped = 0;
        int duplicates = 0;
        int failed = 0;

        for (NewsProvider provider : newsProviders) {
            List<ProviderNewsItem> items = provider.fetchLatest();
            fetched += items.size();

            for (ProviderNewsItem item : items) {
                try {
                    String articleUrl = truncate(item.articleUrl(), 1200);
                    if (articleUrl.isEmpty()) {
                        skipped++;
                        log.debug("NEWS_INGEST_SKIP reason=blank_article_url");
                        continue;
                    }

                    String title = truncate(item.title(), 500);
                    if (title.isEmpty()) {
                        skipped++;
                        log.debug("NEWS_INGEST_SKIP reason=blank_title");
                        continue;
                    }

                    if (item.publishedAt() == null) {
                        skipped++;
                        log.debug("NEWS_INGEST_SKIP reason=null_published_at");
                        continue;
                    }

                    if (!newsRelevanceEvaluator.isRelevant(item)) {
                        skipped++;
                        log.debug("NEWS_INGEST_SKIP reason=irrelevant_domain_news");
                        continue;
                    }

                    if (newsArticleRepository.findByArticleUrl(articleUrl).isPresent()) {
                        skipped++;
                        log.debug("NEWS_INGEST_SKIP reason=duplicate_url_precheck");
                        continue;
                    }

                    NewsArticle article = new NewsArticle();
                    article.setExternalId(item.externalId());
                    article.setTitle(title);
                    article.setSummary(truncate(item.summary(), 2000));
                    article.setArticleUrl(articleUrl);
                    article.setSourceName(truncate(item.sourceName(), 150));
                    article.setCategory(item.category());
                    article.setPublishedAt(item.publishedAt());

                    List<String> matchedSymbols = newsInstrumentMatcher.match(article.getTitle(), article.getSummary());
                    article.setRelatedSymbols(new ArrayList<>(matchedSymbols));
                    article.setTopicTags(new ArrayList<>(newsTopicTagger.resolve(
                            article.getCategory(),
                            article.getTitle(),
                            article.getSummary(),
                            matchedSymbols
                    )));
                    String rssImageUrl = item.rssImageUrl();
                    if (rssImageUrl != null && !rssImageUrl.isBlank()) {
                        article.setImageUrl(ArticleImageUrlNormalizer.truncateStored(rssImageUrl));
                    }
                    if (!matchedSymbols.isEmpty()) {
                        log.info(
                                "NEWS_INSTRUMENT_MATCH articleUrl={} matchedSymbols={}",
                                articleUrl,
                                matchedSymbols
                        );
                        newsInstrumentMatchedEventPublisher.publish(articleUrl, matchedSymbols, title, article.getSourceName(), item.publishedAt());
                    }

                    try {
                        NewsArticle savedArticle = newsArticleRepository.save(article);
                        newsTranslationService.pretranslateForArticle(savedArticle);
                        newsArticleImageService.scheduleResolveAfterIngest(
                                savedArticle.getId(),
                                item.summary()
                        );
                        saved++;
                    } catch (DataIntegrityViolationException ex) {
                        duplicates++;
                        log.debug("NEWS_INGEST_DUPLICATE reason=unique_article_url", ex);
                    }
                } catch (Exception ex) {
                    failed++;
                    log.warn("NEWS_INGEST_ITEM_FAILED", ex);
                }
            }
        }

        log.info(
                "NEWS_INGESTION_SUMMARY fetched={} saved={} skipped={} failed={} duplicates={}",
                fetched,
                saved,
                skipped,
                failed,
                duplicates
        );
        return saved;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

}
