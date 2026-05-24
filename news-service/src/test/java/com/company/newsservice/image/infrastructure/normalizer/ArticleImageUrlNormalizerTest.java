package com.company.newsservice.image.infrastructure.normalizer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ArticleImageUrlNormalizerTest {

    @Test
    void resolve_turnsProtocolRelativeUrlIntoHttps() {
        assertEquals(
                "https://cdn.example.com/a.jpg",
                ArticleImageUrlNormalizer.resolve("//cdn.example.com/a.jpg", "https://example.com/article")
        );
    }

    @Test
    void resolve_resolvesRelativePathAgainstArticleUrl() {
        assertEquals(
                "https://example.com/media/hero.png",
                ArticleImageUrlNormalizer.resolve("/media/hero.png", "https://example.com/article")
        );
    }

    @Test
    void resolve_rejectsNonHttpSchemes() {
        assertNull(ArticleImageUrlNormalizer.resolve("javascript:alert(1)", "https://example.com"));
    }

    @Test
    void truncateStored_capsStoredLength() {
        String longUrl = "https://example.com/" + "a".repeat(2100);

        assertEquals(2000, ArticleImageUrlNormalizer.truncateStored(longUrl).length());
    }
}
