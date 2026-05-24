package com.company.newsservice.image.infrastructure.http;

import com.company.newsservice.image.infrastructure.normalizer.ArticleImageUrlNormalizer;
import com.company.newsservice.image.infrastructure.rss.RssDescriptionImageExtractor;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ArticlePageImageFetcherTest {

    @Test
    void extractsOgImageFromHtmlFragment() {
        String html = """
                <html><head>
                <meta property="og:image" content="https://cdn.example.com/hero.jpg"/>
                </head><body></body></html>
                """;
        var document = Jsoup.parse(html, "https://www.example.com/article/1");
        String content = document.select("meta[property=og:image]").attr("content");
        String resolved = ArticleImageUrlNormalizer.resolve(content, "https://www.example.com/article/1");
        assertEquals("https://cdn.example.com/hero.jpg", resolved);
    }

    @Test
    void rssExtractorFindsImgInDescription() {
        var extractor = new RssDescriptionImageExtractor();
        String url = extractor.extract(
                "<p>Lead</p><img src=\"/media/photo.png\" alt=\"x\"/>",
                "https://news.example.com/story"
        );
        assertNotNull(url);
        assertEquals("https://news.example.com/media/photo.png", url);
    }
}
