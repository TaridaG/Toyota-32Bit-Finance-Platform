package com.company.newsservice.ingestion.infrastructure.relevance;

import com.company.newsservice.bootstrap.config.NewsProperties;
import com.company.newsservice.ingestion.domain.ProviderNewsItem;
import com.company.newsservice.query.domain.enums.NewsCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewsRelevanceEvaluatorTest {

    private NewsRelevanceEvaluator evaluator;

    @BeforeEach
    void setUp() {
        NewsProperties props = new NewsProperties();
        NewsProperties.Relevance relevance = props.getRelevance();
        relevance.setEnabled(true);
        relevance.setMinScore(2);
        relevance.setGlobalKeywords(List.of("bist", "nasdaq", "faiz"));
        relevance.setCategoryKeywords(Map.of("STOCK", List.of("hisse", "borsa")));
        relevance.setBlockedKeywords(List.of("kupon", "casino"));

        props.getInstrument().setKeywords(Map.of("BTCUSDT", List.of("bitcoin")));

        evaluator = new NewsRelevanceEvaluator(props);
    }

    @Test
    void acceptsWhenScoreMeetsThreshold() {
        ProviderNewsItem item = item("BIST ve faiz haberi", "hisse analizi");

        assertTrue(evaluator.isRelevant(item));
    }

    @Test
    void rejectsBlockedKeyword() {
        ProviderNewsItem item = item("Casino kupon kampanyası", "bist faiz");

        assertFalse(evaluator.isRelevant(item));
    }

    @Test
    void acceptsAllWhenFilteringDisabled() {
        NewsProperties props = new NewsProperties();
        props.getRelevance().setEnabled(false);
        NewsRelevanceEvaluator disabled = new NewsRelevanceEvaluator(props);

        assertTrue(disabled.isRelevant(item("anything", "")));
    }

    private static ProviderNewsItem item(String title, String summary) {
        return new ProviderNewsItem(
                null,
                title,
                summary,
                "https://example.com/n",
                "src",
                NewsCategory.STOCK,
                Instant.now()
        );
    }
}
