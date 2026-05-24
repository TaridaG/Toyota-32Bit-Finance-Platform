package com.company.newsservice.image.infrastructure.async;

/**
 * Article image resolve işlemi için Spring application event payload'u.
 */
public record NewsArticleImageResolveEvent(Long articleId, String rssSummaryHint) {
}
