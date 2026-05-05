package com.company.newsservice.config;

import com.company.newsservice.domain.enums.NewsCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "news")
public class NewsProperties {

    private Scheduler scheduler = new Scheduler();
    private Rss rss = new Rss();
    private Relevance relevance = new Relevance();
    private Translation translation = new Translation();
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

    @Getter
    @Setter
    public static class Relevance {
        /**
         * Turns domain-focused filtering on/off before persisting articles.
         */
        private boolean enabled = true;

        /**
         * Minimum total score required for an article to be accepted.
         */
        private int minScore = 3;

        /**
         * If true and categoryKeywords contains an entry for article category,
         * at least one category keyword must match.
         */
        private boolean requireCategoryKeywordMatch = false;

        /**
         * High-level market-domain keywords (BIST, Nasdaq, macro, funds, FX, etc.).
         */
        private List<String> globalKeywords = new ArrayList<>();

        /**
         * Category-specific keywords. Keys should use enum names (e.g. STOCK, FX, FUND).
         */
        private Map<String, List<String>> categoryKeywords = new LinkedHashMap<>();

        /**
         * Optional terms that force rejection when found (spam/noise suppression).
         */
        private List<String> blockedKeywords = new ArrayList<>();

        public List<String> safeGlobalKeywords() {
            return globalKeywords == null ? List.of() : globalKeywords;
        }

        public List<String> safeBlockedKeywords() {
            return blockedKeywords == null ? List.of() : blockedKeywords;
        }

        public List<String> keywordsForCategory(String categoryName) {
            if (categoryKeywords == null || categoryKeywords.isEmpty() || categoryName == null || categoryName.isBlank()) {
                return List.of();
            }
            List<String> keywords = categoryKeywords.get(categoryName);
            if (keywords == null) {
                return List.of();
            }
            return Collections.unmodifiableList(keywords);
        }
    }

    @Getter
    @Setter
    public static class Translation {
        /**
         * Enables translated payload generation.
         */
        private boolean enabled = true;

        /**
         * Provider id: mymemory or noop.
         */
        private String provider = "mymemory";

        /**
         * User selectable target languages that we support.
         */
        private List<String> supportedLanguages = new ArrayList<>(List.of("tr", "en", "de"));

        /**
         * Fallback language when request does not provide a supported language.
         */
        private String defaultLanguage = "en";
        private boolean backfillEnabled = true;
        private int backfillBatchSize = 50;
        private long backfillDelayMs = 60000L;

        private Mymemory mymemory = new Mymemory();
    }

    @Getter
    @Setter
    public static class Mymemory {
        private String baseUrl = "https://api.mymemory.translated.net";
        private int timeoutMs = 5000;
        /**
         * Optional email can increase free quota on MyMemory.
         */
        private String email = "";
    }
}