package com.company.newsservice.bootstrap.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(classes = NewsPropertiesBindingTest.Config.class)
@TestPropertySource(properties = {
        "news.instrument.keywords.BTCUSDT[0]=bitcoin",
        "news.instrument.keywords.BTCUSDT[1]=btc"
})
class NewsPropertiesBindingTest {

    @Autowired
    private NewsProperties newsProperties;

    @Test
    void bindsNestedInstrumentKeywordsFromYamlShape() {
        assertFalse(newsProperties.getInstrumentKeywords().isEmpty());
        assertEquals(2, newsProperties.getInstrumentKeywords().get("BTCUSDT").size());
    }

    @EnableConfigurationProperties(NewsProperties.class)
    static class Config {
    }
}
