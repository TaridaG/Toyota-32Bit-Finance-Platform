package com.company.newsservice.image;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Extracts the first usable image URL from RSS/HTML description without fetching the article page.
 */
@Component
public class RssDescriptionImageExtractor {

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
