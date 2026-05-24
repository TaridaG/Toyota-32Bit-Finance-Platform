package com.company.newsservice.ingestion.infrastructure.matching;

import com.company.newsservice.bootstrap.config.NewsProperties;
import com.company.newsservice.query.domain.enums.NewsCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NewsTopicTaggerTest {

    private NewsTopicTagger tagger;

    @BeforeEach
    void setUp() {
        NewsProperties props = new NewsProperties();
        props.getTopicTags().setKeywords(Map.of(
                "crypto", List.of("bitcoin", "ethereum"),
                "macro", List.of("fed", "enflasyon")
        ));
        tagger = new NewsTopicTagger(props);
    }

    @Test
    void cryptoCategoryIncludesCryptoTag() {
        List<String> tags = tagger.resolve(NewsCategory.CRYPTO, "Bitcoin rally", "", List.of("BTCUSDT"));

        assertTrue(tags.contains("crypto"));
    }

    @Test
    void macroHeadlineAddsCrossMarketTags() {
        List<String> tags = tagger.resolve(
                NewsCategory.GENERAL_ECONOMY,
                "Fed faiz kararı Türkiye borsasını etkiledi",
                "",
                List.of()
        );

        assertTrue(tags.contains("macro"));
        assertTrue(tags.contains("fx"));
        assertTrue(tags.contains("bist"));
    }
}
