package com.company.newsservice.provider.rss;

import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * RSS feeds (e.g. Bigpara) often ship root-relative article paths ({@code /haberler/...}).
 * Browsers then open them on the portal host instead of the publisher site.
 */
public final class ArticleUrlNormalizer {

    private ArticleUrlNormalizer() {
    }

    public static String normalize(String rawUrl, String feedUrl, String sourceName) {
        if (!StringUtils.hasText(rawUrl)) {
            return "";
        }
        String trimmed = rawUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("//")) {
            return "https:" + trimmed;
        }
        String base = resolvePublisherBase(feedUrl, sourceName);
        if (!StringUtils.hasText(base)) {
            return trimmed;
        }
        try {
            URI baseUri = URI.create(base.endsWith("/") ? base : base + "/");
            String path = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
            return baseUri.resolve(path).toString();
        } catch (Exception ex) {
            return trimmed;
        }
    }

    private static String resolvePublisherBase(String feedUrl, String sourceName) {
        if (StringUtils.hasText(feedUrl)) {
            try {
                URI feed = URI.create(feedUrl.trim());
                if (StringUtils.hasText(feed.getScheme()) && StringUtils.hasText(feed.getHost())) {
                    return feed.getScheme() + "://" + feed.getHost();
                }
            } catch (Exception ignored) {
                // fall through
            }
        }
        if (!StringUtils.hasText(sourceName)) {
            return "";
        }
        String source = sourceName.trim().toLowerCase();
        if (source.contains("bigpara")) {
            return "https://bigpara.hurriyet.com.tr";
        }
        if (source.contains("cointelegraph")) {
            return "https://cointelegraph.com";
        }
        return "";
    }
}
