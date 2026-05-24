package com.company.newsservice.image.infrastructure.normalizer;

import java.net.URI;

/**
 * Article image URL'lerini normalize eder ve persistence için kısaltır.
 */
public final class ArticleImageUrlNormalizer {

    private static final int MAX_LENGTH = 2000;

    private ArticleImageUrlNormalizer() {
    }

    /** Ham URL'yi base URL'ye göre absolute http/https URI'ye çevirir. */
    public static String resolve(String raw, String baseUrl) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String candidate = raw.trim();
        if (candidate.startsWith("//")) {
            candidate = "https:" + candidate;
        }
        try {
            URI base = baseUrl != null && !baseUrl.isBlank() ? URI.create(baseUrl.trim()) : null;
            URI uri = base != null ? base.resolve(candidate) : URI.create(candidate);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return null;
            }
            String normalized = uri.toString().trim();
            if (normalized.length() > MAX_LENGTH) {
                normalized = normalized.substring(0, MAX_LENGTH);
            }
            return normalized;
        } catch (Exception ex) {
            return null;
        }
    }

    /** DB'de saklanacak image URL uzunluğunu sınırlar. */
    public static String truncateStored(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= MAX_LENGTH ? trimmed : trimmed.substring(0, MAX_LENGTH);
    }
}
