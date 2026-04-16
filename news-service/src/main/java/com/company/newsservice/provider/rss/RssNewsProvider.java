package com.company.newsservice.provider.rss;

import com.company.newsservice.config.NewsProperties;
import com.company.newsservice.provider.NewsProvider;
import com.company.newsservice.provider.ProviderNewsItem;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RssNewsProvider implements NewsProvider {

    private final NewsProperties newsProperties;

    @Override
    public String providerType() {
        return "RSS";
    }

    @Override
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
                        items.add(new ProviderNewsItem(
                                entry.getUri(),
                                safe(entry.getTitle()),
                                safe(entry.getDescription() != null ? entry.getDescription().getValue() : null),
                                safe(entry.getLink()),
                                feedConfig.getName(),
                                feedConfig.getCategory(),
                                entry.getPublishedDate() != null
                                        ? entry.getPublishedDate().toInstant()
                                        : Instant.now()
                        ));
                    }
                }
            } catch (Exception ex) {
                log.warn("RSS fetch failed. feedName={}, url={}", feedConfig.getName(), feedConfig.getUrl(), ex);
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