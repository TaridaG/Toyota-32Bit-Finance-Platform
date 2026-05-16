package com.company.newsservice.service;

import com.company.newsservice.domain.NewsArticle;
import com.company.newsservice.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NewsRelatedSymbolsResolver {

    private final NewsInstrumentMatcher newsInstrumentMatcher;
    private final NewsArticleRepository newsArticleRepository;

    public List<String> readOrMatch(NewsArticle article) {
        List<String> stored = article.getRelatedSymbols();
        if (stored != null && !stored.isEmpty()) {
            return List.copyOf(stored);
        }
        return newsInstrumentMatcher.match(article.getTitle(), article.getSummary());
    }

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
