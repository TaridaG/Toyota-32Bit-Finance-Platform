package com.company.newsservice.ingestion.infrastructure.rss;

import com.company.newsservice.bootstrap.config.NewsProperties;
import com.company.newsservice.image.infrastructure.rss.RssDescriptionImageExtractor;
import com.company.newsservice.ingestion.domain.ProviderNewsItem;
import com.company.newsservice.query.domain.enums.NewsCategory;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RssNewsProviderTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchLatest_parsesFeedFromHttpServer() {
        serveRss("""
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                  <channel>
                    <title>Test Feed</title>
                    <item>
                      <title>Sample Title</title>
                      <link>https://example.com/news/1</link>
                      <description>Summary text</description>
                      <pubDate>Wed, 22 May 2026 10:00:00 GMT</pubDate>
                    </item>
                  </channel>
                </rss>
                """);

        RssNewsProvider provider = providerWithFeed("http://127.0.0.1:" + port + "/feed");

        List<ProviderNewsItem> items = provider.fetchLatest();

        assertEquals(1, items.size());
        ProviderNewsItem item = items.getFirst();
        assertEquals("Sample Title", item.title());
        assertEquals("Summary text", item.summary());
        assertEquals("TestFeed", item.sourceName());
        assertEquals(NewsCategory.STOCK, item.category());
        assertEquals("https://example.com/news/1", item.articleUrl());
    }

    @Test
    void fetchLatest_returnsEmptyWhenFeedUnavailable() {
        RssNewsProvider provider = providerWithFeed("http://127.0.0.1:" + port + "/missing");

        assertTrue(provider.fetchLatest().isEmpty());
    }

    @Test
    void providerType_isRss() {
        RssNewsProvider provider = providerWithFeed("http://127.0.0.1:" + port + "/feed");
        assertEquals("RSS", provider.providerType());
    }

    private RssNewsProvider providerWithFeed(String feedUrl) {
        NewsProperties properties = new NewsProperties();
        NewsProperties.Feed feed = new NewsProperties.Feed();
        feed.setName("TestFeed");
        feed.setUrl(feedUrl);
        feed.setCategory(NewsCategory.STOCK);
        properties.setFeeds(List.of(feed));

        return new RssNewsProvider(
                properties,
                new RssEntryImageExtractor(new RssDescriptionImageExtractor())
        );
    }

    private void serveRss(String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        server.createContext("/feed", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/rss+xml; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }
}
