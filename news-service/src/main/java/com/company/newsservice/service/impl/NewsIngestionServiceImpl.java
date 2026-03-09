package com.company.newsservice.service.impl;

import com.company.newsservice.domain.NewsArticle;
import com.company.newsservice.provider.NewsProvider;
import com.company.newsservice.provider.ProviderNewsItem;
import com.company.newsservice.repository.NewsArticleRepository;
import com.company.newsservice.service.NewsIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsIngestionServiceImpl implements NewsIngestionService {

    private final List<NewsProvider> newsProviders;
    private final NewsArticleRepository newsArticleRepository;

    @Override
    @Transactional
    public int ingestLatest() {
        int createdCount = 0;

        for (NewsProvider provider : newsProviders) {
            List<ProviderNewsItem> items = provider.fetchLatest();

            for (ProviderNewsItem item : items) {
                if (item.articleUrl() == null || item.articleUrl().isBlank()) {
                    continue;
                }

                boolean exists = newsArticleRepository.findByArticleUrl(item.articleUrl()).isPresent();
                if (exists) {
                    continue;
                }

                NewsArticle article = new NewsArticle();
                article.setExternalId(item.externalId());
                article.setTitle(truncate(item.title(), 500));
                article.setSummary(truncate(item.summary(), 2000));
                article.setArticleUrl(truncate(item.articleUrl(), 1200));
                article.setSourceName(truncate(item.sourceName(), 150));
                article.setCategory(item.category());
                article.setPublishedAt(item.publishedAt());

                newsArticleRepository.save(article);
                createdCount++;
            }
        }

        log.info("News ingestion completed. createdCount={}", createdCount);
        return createdCount;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}