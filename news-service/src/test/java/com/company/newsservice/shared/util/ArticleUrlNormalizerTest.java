package com.company.newsservice.shared.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArticleUrlNormalizerTest {

    @Test
    void bigparaRelativePath_resolvesToPublisherHost() {
        String resolved = ArticleUrlNormalizer.normalize(
                "/haberler/ekonomi-haberleri/faiz-beklentisine-2-aylik-savas-freni_ID100701638/",
                "https://bigpara.hurriyet.com.tr/rss",
                "Bigpara"
        );
        assertEquals(
                "https://bigpara.hurriyet.com.tr/haberler/ekonomi-haberleri/faiz-beklentisine-2-aylik-savas-freni_ID100701638/",
                resolved
        );
    }

    @Test
    void absoluteUrl_unchanged() {
        String url = "https://cointelegraph.com/news/btc";
        assertEquals(url, ArticleUrlNormalizer.normalize(url, "https://cointelegraph.com/rss", "CoinTelegraph"));
    }
}
