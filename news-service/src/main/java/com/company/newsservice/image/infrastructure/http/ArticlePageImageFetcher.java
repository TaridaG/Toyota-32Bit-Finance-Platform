package com.company.newsservice.image.infrastructure.http;

import com.company.newsservice.bootstrap.config.NewsProperties;
import com.company.newsservice.image.infrastructure.normalizer.ArticleImageUrlNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;

/**
 * Article source URL'den Open Graph / Twitter image metadata fetch eder (read-only HTML GET).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticlePageImageFetcher {

    private static final List<String> META_SELECTORS = List.of(
            "meta[property=og:image:secure_url]",
            "meta[property=og:image]",
            "meta[name=og:image]",
            "meta[property=twitter:image]",
            "meta[name=twitter:image]",
            "meta[property=twitter:image:src]",
            "meta[name=twitter:image:src]",
            "link[rel=image_src]"
    );

    private final NewsProperties newsProperties;

    /** Article sayfasından ilk kullanılabilir image URL'yi döner. */
    public String fetch(String articleUrl) {
        if (articleUrl == null || articleUrl.isBlank()) {
            return null;
        }
        NewsProperties.Image cfg = newsProperties.getImage();
        if (cfg == null || !cfg.isEnabled()) {
            return null;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(articleUrl.trim()).toURL().openConnection();
            connection.setConnectTimeout(cfg.getConnectTimeoutMs());
            connection.setReadTimeout(cfg.getReadTimeoutMs());
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", cfg.getUserAgent());
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml");

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                return null;
            }

            try (InputStream inputStream = connection.getInputStream()) {
                Document document = Jsoup.parse(inputStream, null, articleUrl);
                for (String selector : META_SELECTORS) {
                    for (Element element : document.select(selector)) {
                        String content = element.hasAttr("content")
                                ? element.attr("content")
                                : element.attr("href");
                        String resolved = ArticleImageUrlNormalizer.resolve(content, articleUrl);
                        if (resolved != null) {
                            return resolved;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("NEWS_IMAGE_PAGE_FETCH_FAILED url={} reason={}", articleUrl, ex.toString());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }
}
