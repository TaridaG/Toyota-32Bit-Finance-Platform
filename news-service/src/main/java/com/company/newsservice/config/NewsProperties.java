package com.company.newsservice.config;

import com.company.newsservice.domain.enums.NewsCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "news")
public class NewsProperties {

    private Scheduler scheduler = new Scheduler();
    private Rss rss = new Rss();
    private List<Feed> feeds = new ArrayList<>();

    /**
     * Symbol (e.g. BTCUSDT) → lowercase substring keywords for ingest-time matching only.
     */
    private Map<String, List<String>> instrumentKeywords = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class Scheduler {
        private boolean enabled = true;
        private long delayMs = 300000; // 5 dk
    }

    @Getter
    @Setter
    public static class Feed {
        @NotBlank
        private String name;

        @NotBlank
        private String url;

        private NewsCategory category = NewsCategory.OTHER;
    }

    @Getter
    @Setter
    public static class Rss {
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 10000;
        private int maxEntriesPerFeed = 100;
        private String userAgent = "finance-news-service/1.0";
    }
}