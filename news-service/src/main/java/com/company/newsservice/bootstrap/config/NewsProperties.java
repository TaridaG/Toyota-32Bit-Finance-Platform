package com.company.newsservice.bootstrap.config;

import com.company.newsservice.query.domain.enums.NewsCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RSS ingestion, relevance, çeviri ve instrument eşleştirme ayarları ({@code news.*}).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "news")
public class NewsProperties {

    private Scheduler scheduler = new Scheduler();
    private Rss rss = new Rss();
    private Relevance relevance = new Relevance();
    private TopicTags topicTags = new TopicTags();
    private Translation translation = new Translation();
    private Image image = new Image();
    private List<Feed> feeds = new ArrayList<>();

    private Instrument instrument = new Instrument();

    /**
     * Eski düz binding ({@code news.instrument-keywords.*}); tercih edilen {@link Instrument#getKeywords()}.
     */
    @Getter(AccessLevel.NONE)
    @Setter
    private Map<String, List<String>> instrumentKeywords = new LinkedHashMap<>();

    /**
     * Instrument keyword eşleştirmesi için çözümlenmiş keyword map'i (YAML: {@code news.instrument.keywords}).
     */
    public Map<String, List<String>> getInstrumentKeywords() {
        if (instrument != null && instrument.getKeywords() != null && !instrument.getKeywords().isEmpty()) {
            return instrument.getKeywords();
        }
        return instrumentKeywords == null ? Map.of() : instrumentKeywords;
    }

    /** Instrument sembol → title/summary'de aranan lowercase substring keyword eşleştirmesi. */
    @Getter
    @Setter
    public static class Instrument {
        /** Sembol (örn. BTCUSDT) → lowercase substring keyword listesi. */
        private Map<String, List<String>> keywords = new LinkedHashMap<>();
    }

    /** RSS ingestion scheduler ayarları. */
    @Getter
    @Setter
    public static class Scheduler {
        /** Scheduler'ın aktif olup olmadığı. */
        private boolean enabled = true;
        /** İki ingestion çalıştırması arasındaki gecikme (ms). */
        private long delayMs = 300000; // 5 dk
    }

    /** Tek RSS feed kaynağı tanımı. */
    @Getter
    @Setter
    public static class Feed {
        /** Feed görünen adı. */
        @NotBlank
        private String name;

        /** RSS/Atom feed URL'i. */
        @NotBlank
        private String url;

        /** Feed'in varsayılan haber kategorisi. */
        private NewsCategory category = NewsCategory.OTHER;
    }

    /** RSS fetch HTTP timeout ve parsing limitleri. */
    @Getter
    @Setter
    public static class Rss {
        /** TCP connect timeout (ms). */
        private int connectTimeoutMs = 5000;
        /** HTTP read timeout (ms). */
        private int readTimeoutMs = 10000;
        /** Feed başına işlenecek maksimum entry sayısı. */
        private int maxEntriesPerFeed = 100;
        /** Outbound HTTP User-Agent. */
        private String userAgent = "finance-news-service/1.0";
    }

    /** Makale relevance skorlama ve domain filtresi ayarları. */
    @Getter
    @Setter
    public static class Relevance {
        /**
         * Persist etmeden önce domain odaklı filtrelemenin açık/kapalı durumu.
         */
        private boolean enabled = true;

        /**
         * Makalenin kabul edilmesi için gereken minimum toplam skor.
         */
        private int minScore = 3;

        /**
         * {@code true} ise ve categoryKeywords ilgili kategori için tanımlıysa,
         * en az bir kategori keyword'ü eşleşmelidir.
         */
        private boolean requireCategoryKeywordMatch = false;

        /**
         * Genel piyasa domain keyword'leri (BIST, Nasdaq, makro, fon, FX vb.).
         */
        private List<String> globalKeywords = new ArrayList<>();

        /**
         * Kategori bazlı keyword'ler. Anahtarlar enum adı olmalı (örn. STOCK, FX, FUND).
         */
        private Map<String, List<String>> categoryKeywords = new LinkedHashMap<>();

        /**
         * Bulunduğunda makaleyi reddeden isteğe bağlı terimler (spam/gürültü baskılama).
         */
        private List<String> blockedKeywords = new ArrayList<>();

        /** Null-safe global keyword listesi. */
        public List<String> safeGlobalKeywords() {
            return globalKeywords == null ? List.of() : globalKeywords;
        }

        /** Null-safe blocked keyword listesi. */
        public List<String> safeBlockedKeywords() {
            return blockedKeywords == null ? List.of() : blockedKeywords;
        }

        /** Verilen kategori adı için yapılandırılmış keyword listesini döner. */
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

    /** UI topic tag eşleştirme keyword'leri ({@code bist}, {@code fx}, {@code crypto} vb.). */
    @Getter
    @Setter
    public static class TopicTags {
        /**
         * UI topic id → lowercase keyword'ler (bist, fx, crypto, macro, viop).
         */
        private Map<String, List<String>> keywords = new LinkedHashMap<>();

        /** Null-safe topic keyword map'i. */
        public Map<String, List<String>> safeKeywords() {
            return keywords == null || keywords.isEmpty() ? Map.of() : keywords;
        }
    }

    /** Makale çeviri provider ve backfill ayarları. */
    @Getter
    @Setter
    public static class Translation {
        /**
         * Çevrilmiş payload üretiminin açık/kapalı durumu.
         */
        private boolean enabled = true;

        /**
         * Provider kimliği: mymemory veya noop.
         */
        private String provider = "mymemory";

        /**
         * Desteklenen hedef diller.
         */
        private List<String> supportedLanguages = new ArrayList<>(List.of("tr", "en", "de"));

        /**
         * İstek desteklenmeyen dil içerdiğinde kullanılan fallback dil.
         */
        private String defaultLanguage = "en";
        /** Eksik çevirileri arka planda tamamlama scheduler'ının aktif olup olmadığı. */
        private boolean backfillEnabled = true;
        /** Backfill batch boyutu. */
        private int backfillBatchSize = 50;
        /** Backfill çalışmaları arası gecikme (ms). */
        private long backfillDelayMs = 60000L;

        private Mymemory mymemory = new Mymemory();
    }

    /** Makale hero/thumbnail image resolve ve backfill ayarları. */
    @Getter
    @Setter
    public static class Image {
        /**
         * {@code true} ise ingest sonrası {@code image_url} RSS description ve/veya makale sayfası metadata'sından çözülür.
         */
        private boolean enabled = true;
        /** Image fetch TCP connect timeout (ms). */
        private int connectTimeoutMs = 5000;
        /** Image fetch HTTP read timeout (ms). */
        private int readTimeoutMs = 10000;
        /** Outbound HTTP User-Agent. */
        private String userAgent = "finance-news-service/1.0";
        /** Eksik görselleri arka planda tamamlama scheduler'ının aktif olup olmadığı. */
        private boolean backfillEnabled = true;
        /** Image backfill batch boyutu. */
        private int backfillBatchSize = 25;
        /** Image backfill çalışmaları arası gecikme (ms). */
        private long backfillDelayMs = 120000L;
    }

    /** MyMemory çeviri API bağlantı ayarları. */
    @Getter
    @Setter
    public static class Mymemory {
        /** MyMemory REST API base URL. */
        private String baseUrl = "https://api.mymemory.translated.net";
        /** HTTP request timeout (ms). */
        private int timeoutMs = 5000;
        /**
         * İsteğe bağlı e-posta; MyMemory ücretsiz kotasını artırabilir.
         */
        private String email = "";
    }
}