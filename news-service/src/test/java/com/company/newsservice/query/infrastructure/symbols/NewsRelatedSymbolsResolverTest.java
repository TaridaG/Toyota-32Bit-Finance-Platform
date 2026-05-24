package com.company.newsservice.query.infrastructure.symbols;

import com.company.newsservice.ingestion.infrastructure.matching.NewsInstrumentMatcher;
import com.company.newsservice.query.domain.NewsArticle;
import com.company.newsservice.query.infrastructure.persistence.NewsArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsRelatedSymbolsResolverTest {

    @Mock
    private NewsInstrumentMatcher newsInstrumentMatcher;
    @Mock
    private NewsArticleRepository newsArticleRepository;

    @InjectMocks
    private NewsRelatedSymbolsResolver resolver;

    @Test
    void readOrMatch_returnsStoredSymbolsWithoutMatching() {
        NewsArticle article = new NewsArticle();
        article.setRelatedSymbols(List.of("BTCUSDT"));

        assertEquals(List.of("BTCUSDT"), resolver.readOrMatch(article));

        verify(newsInstrumentMatcher, never()).match(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void resolveAndPersist_persistsWhenMatcherFindsSymbols() {
        NewsArticle article = new NewsArticle();
        article.setTitle("Bitcoin rally");
        article.setSummary("");
        article.setRelatedSymbols(new ArrayList<>());
        when(newsInstrumentMatcher.match("Bitcoin rally", "")).thenReturn(List.of("BTCUSDT"));
        when(newsArticleRepository.save(article)).thenReturn(article);

        List<String> resolved = resolver.resolveAndPersist(article);

        assertEquals(List.of("BTCUSDT"), resolved);
        assertEquals(List.of("BTCUSDT"), article.getRelatedSymbols());
        verify(newsArticleRepository).save(article);
    }
}
