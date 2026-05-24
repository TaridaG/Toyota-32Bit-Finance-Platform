package com.company.newsservice.image.infrastructure.rss;

import com.company.newsservice.image.infrastructure.normalizer.ArticleImageUrlNormalizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Article page fetch etmeden RSS/HTML description içinden ilk kullanılabilir image URL'yi çıkarır.
 */
@Component
public class RssDescriptionImageExtractor {

    /** HTML veya text içinden ilk kullanılabilir image URL'yi extract eder. */
    public String extract(String htmlOrText, String articleUrl) {
        if (htmlOrText == null || htmlOrText.isBlank()) {
            return null;
        }
        String trimmed = htmlOrText.trim();
        if (!trimmed.contains("<")) {
            return null;
        }
        try {
            Document doc = Jsoup.parseBodyFragment(trimmed);
            for (Element img : doc.select("img[src]")) {
                String resolved = ArticleImageUrlNormalizer.resolve(img.attr("src"), articleUrl);
                if (resolved != null) {
                    return resolved;
                }
            }
            for (Element source : doc.select("source[src]")) {
                String resolved = ArticleImageUrlNormalizer.resolve(source.attr("src"), articleUrl);
                if (resolved != null) {
                    return resolved;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
