package com.company.newsservice.query.infrastructure.symbols;

import com.company.newsservice.query.domain.NewsArticle;
import com.company.newsservice.query.infrastructure.persistence.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import com.company.newsservice.ingestion.infrastructure.matching.NewsInstrumentMatcher;

/**
 * Haber başlık/özet metninden ilişkili enstrüman sembollerini eşleştirir ve isteğe bağlı persist eder.
 */
@Component
@RequiredArgsConstructor
public class NewsRelatedSymbolsResolver {

    private final NewsInstrumentMatcher newsInstrumentMatcher;
    private final NewsArticleRepository newsArticleRepository;

    /** Persist edilmiş sembolleri döner; yoksa matcher ile anlık eşleştirir (kaydetmez). */
    public List<String> readOrMatch(NewsArticle article) {
        List<String> stored = article.getRelatedSymbols();
        if (stored != null && !stored.isEmpty()) {
            return List.copyOf(stored);
        }
        return newsInstrumentMatcher.match(article.getTitle(), article.getSummary());
    }

    /**
     * Sembolleri çözümler; entity'de yoksa matcher sonucunu {@code related_symbols} olarak persist eder.
     */
    @Transactional
    public List<String> resolveAndPersist(NewsArticle article) {
        List<String> stored = article.getRelatedSymbols();
        if (stored != null && !stored.isEmpty()) {
            return List.copyOf(stored);
        }
        List<String> matched = newsInstrumentMatcher.match(article.getTitle(), article.getSummary());
        if (matched.isEmpty()) {
            return List.of();
        }
        article.setRelatedSymbols(new ArrayList<>(matched));
        newsArticleRepository.save(article);
        return List.copyOf(matched);
    }
}
