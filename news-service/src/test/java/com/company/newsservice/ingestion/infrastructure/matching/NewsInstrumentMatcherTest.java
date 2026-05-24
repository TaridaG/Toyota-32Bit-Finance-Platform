package com.company.newsservice.ingestion.infrastructure.matching;

import com.company.newsservice.bootstrap.config.NewsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewsInstrumentMatcherTest {

    private NewsInstrumentMatcher matcher;

    @BeforeEach
    void setUp() {
        NewsProperties props = new NewsProperties();
        Map<String, List<String>> kw = new LinkedHashMap<>();
        kw.put("BTCUSDT", List.of("bitcoin", "btc"));
        kw.put("ETHUSDT", List.of("ethereum", "eth"));
        kw.put("USDTRY", List.of("usd", "dolar"));
        kw.put("GARAN", List.of("garanti"));
        kw.put("THYAO", List.of("thy", "turkish airlines"));
        props.getInstrument().setKeywords(kw);
        matcher = new NewsInstrumentMatcher(props);
    }

    @Test
    void bitcoin_mapsToBtcusdt() {
        assertEquals(List.of("BTCUSDT"), matcher.match("About Bitcoin prices", ""));
    }

    @Test
    void dolar_mapsToUsdtry() {
        assertEquals(List.of("USDTRY"), matcher.match("Dolar yükseldi", null));
    }

    @Test
    void emptyText_returnsEmpty() {
        assertTrue(matcher.match("", "").isEmpty());
        assertTrue(matcher.match("   ", "   ").isEmpty());
    }
}
