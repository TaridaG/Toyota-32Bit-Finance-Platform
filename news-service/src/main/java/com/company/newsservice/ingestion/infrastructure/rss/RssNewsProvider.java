package com.company.newsservice.ingestion.infrastructure.rss;

import com.company.newsservice.bootstrap.config.NewsProperties;
import com.company.newsservice.ingestion.domain.NewsProvider;
import com.company.newsservice.ingestion.domain.ProviderNewsItem;
import com.company.newsservice.shared.util.ArticleUrlNormalizer;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Yapılandırılmış RSS feed'lerinden haber çeken {@link NewsProvider} implementasyonu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RssNewsProvider implements NewsProvider {

    private final NewsProperties newsProperties;
    private final RssEntryImageExtractor rssEntryImageExtractor;
    private final Map<String, Instant> lastRssErrorAt = new ConcurrentHashMap<>();
    private static final Duration RSS_ERROR_LOG_WINDOW = Duration.ofMinutes(5);

    /** {@inheritDoc} */
    public String providerType() {
        return "RSS";
    }

    /** {@inheritDoc} */
    public List<ProviderNewsItem> fetchLatest() {
        List<ProviderNewsItem> items = new ArrayList<>();

        for (NewsProperties.Feed feedConfig : newsProperties.getFeeds()) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) URI.create(feedConfig.getUrl()).toURL().openConnection();
                connection.setConnectTimeout(newsProperties.getRss().getConnectTimeoutMs());
                connection.setReadTimeout(newsProperties.getRss().getReadTimeoutMs());
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", newsProperties.getRss().getUserAgent());

                try (InputStream inputStream = connection.getInputStream()) {

                    SyndFeed feed = new SyndFeedInput().build(new com.rometools.rome.io.XmlReader(inputStream));

                    int maxEntries = Math.max(newsProperties.getRss().getMaxEntriesPerFeed(), 1);
                    for (SyndEntry entry : feed.getEntries().stream().limit(maxEntries).toList()) {
                        String articleUrl = ArticleUrlNormalizer.normalize(
                                safe(entry.getLink()),
                                feedConfig.getUrl(),
                                feedConfig.getName()
                        );
                        items.add(new ProviderNewsItem(
                                entry.getUri(),
                                safe(entry.getTitle()),
                                safe(entry.getDescription() != null ? entry.getDescription().getValue() : null),
                                articleUrl,
                                feedConfig.getName(),
                                feedConfig.getCategory(),
                                entry.getPublishedDate() != null
                                        ? entry.getPublishedDate().toInstant()
                                        : Instant.now(),
                                rssEntryImageExtractor.extract(entry, articleUrl)
                        ));
                    }
                }
            } catch (Exception ex) {
                String feedKey = feedConfig.getUrl();
                Instant now = Instant.now();
                Instant lastLoggedAt = lastRssErrorAt.get(feedKey);
                boolean shouldWarn = lastLoggedAt == null ||
                        Duration.between(lastLoggedAt, now).compareTo(RSS_ERROR_LOG_WINDOW) >= 0;
                if (shouldWarn) {
                    lastRssErrorAt.put(feedKey, now);
                    log.warn("RSS fetch failed. feedName={}, url={}", feedConfig.getName(), feedConfig.getUrl(), ex);
                } else {
                    log.debug("RSS fetch failed (suppressed). feedName={}, url={}",
                            feedConfig.getName(), feedConfig.getUrl());
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        return items;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}