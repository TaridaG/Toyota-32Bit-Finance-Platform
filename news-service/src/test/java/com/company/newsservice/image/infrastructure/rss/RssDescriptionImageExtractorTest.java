package com.company.newsservice.image.infrastructure.rss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RssDescriptionImageExtractorTest {

    private final RssDescriptionImageExtractor extractor = new RssDescriptionImageExtractor();

    @Test
    void extract_returnsFirstValidImageFromHtml() {
        String html = "<p>text</p><img src=\"/images/a.jpg\" alt=\"hero\"/>";

        assertEquals(
                "https://news.example.com/images/a.jpg",
                extractor.extract(html, "https://news.example.com/article/1")
        );
    }

    @Test
    void extract_returnsNullForPlainText() {
        assertNull(extractor.extract("No markup here", "https://example.com"));
    }

    @Test
    void extract_returnsNullWhenNoUsableImage() {
        assertNull(extractor.extract("<img src=\"javascript:alert(1)\"/>", "https://example.com"));
    }
}
