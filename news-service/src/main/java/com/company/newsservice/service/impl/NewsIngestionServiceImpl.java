package com.company.newsservice.service.impl;

import com.company.newsservice.domain.NewsArticle;
import com.company.newsservice.event.NewsInstrumentMatchedEvent;
import com.company.newsservice.provider.NewsProvider;
import com.company.newsservice.provider.ProviderNewsItem;
import com.company.newsservice.repository.NewsArticleRepository;
import com.company.newsservice.service.NewsIngestionService;
import com.company.newsservice.service.NewsInstrumentMatcher;
import com.company.newsservice.service.NewsRelevanceEvaluator;
import com.company.newsservice.service.NewsTopicTagger;
import com.company.newsservice.service.translation.NewsTranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsIngestionServiceImpl implements NewsIngestionService {

    public static final String TOPIC_NEWS_INSTRUMENT_MATCHED = "news.instrument.matched";

    private final List<NewsProvider> newsProviders;
    private final NewsArticleRepository newsArticleRepository;
    private final NewsInstrumentMatcher newsInstrumentMatcher;
    private final NewsRelevanceEvaluator newsRelevanceEvaluator;
    private final NewsTopicTagger newsTopicTagger;
    private final NewsTranslationService newsTranslationService;
    private final KafkaTemplate<String, Object> newsKafkaTemplate;

    @Override
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
                    if (!matchedSymbols.isEmpty()) {
                        log.info(
                                "NEWS_INSTRUMENT_MATCH articleUrl={} matchedSymbols={}",
                                articleUrl,
                                matchedSymbols
                        );
                        publishNewsInstrumentMatched(
                                articleUrl,
                                matchedSymbols,
                                title,
                                article.getSourceName(),
                                item.publishedAt()
                        );
                    }

                    try {
                        NewsArticle savedArticle = newsArticleRepository.save(article);
                        newsTranslationService.pretranslateForArticle(savedArticle);
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

    private void publishNewsInstrumentMatched(
            String articleUrl,
            List<String> matchedSymbols,
            String title,
            String sourceName,
            Instant publishedAt
    ) {
        try {
            NewsInstrumentMatchedEvent payload =
                    NewsInstrumentMatchedEvent.of(matchedSymbols, title, sourceName, publishedAt);
            newsKafkaTemplate.send(TOPIC_NEWS_INSTRUMENT_MATCHED, articleUrl, payload);
        } catch (Exception ex) {
            log.warn("NEWS_INSTRUMENT_MATCHED_PUBLISH_FAILED title={} reason={}", title, ex.toString());
        }
    }
}
