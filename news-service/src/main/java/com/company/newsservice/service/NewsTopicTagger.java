package com.company.newsservice.service;

import com.company.newsservice.config.NewsProperties;
import com.company.newsservice.domain.enums.NewsCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Derives UI topic tags ({@code bist}, {@code fx}, {@code crypto}, {@code macro}, {@code viop})
 * from article text, wire category, and matched symbols. Supports cross-market impact
 * (e.g. Fed / war / commodities → macro + fx + bist when Turkey is in scope).
 */
@Service
@RequiredArgsConstructor
public class NewsTopicTagger {

    private static final Set<String> FX_SYMBOL_HINTS = Set.of(
            "USDTRY", "EURTRY", "GBPTRY", "XAUUSD", "XAGUSD", "XAUTRY", "XAGTRY", "GLDTR", "USD", "EUR", "TRY"
    );
    private static final Set<String> CRYPTO_SYMBOL_HINTS = Set.of("BTC", "ETH", "BTCUSDT", "ETHUSDT", "BNB", "SOL", "XRP");

    private final NewsProperties newsProperties;

    public List<String> resolve(
            NewsCategory primaryCategory,
            String title,
            String summary,
            List<String> relatedSymbols
    ) {
        Set<String> tags = new LinkedHashSet<>();
        tags.add(primaryUiTag(primaryCategory));

        String haystack = normalizeHaystack(title, summary);
        Map<String, List<String>> topicKeywords = newsProperties.getTopicTags().safeKeywords();
        for (Map.Entry<String, List<String>> entry : topicKeywords.entrySet()) {
            if (containsAny(haystack, entry.getValue())) {
                tags.add(entry.getKey());
            }
        }

        applySymbolHints(tags, relatedSymbols);
        applyCrossMarketRules(tags, haystack, primaryCategory);

        return List.copyOf(tags);
    }

    private static String primaryUiTag(NewsCategory category) {
        if (category == null) {
            return "macro";
        }
        return switch (category) {
            case CRYPTO -> "crypto";
            case FX -> "fx";
            case STOCK -> "bist";
            case FUND, BOND, GENERAL_ECONOMY -> "macro";
            default -> "macro";
        };
    }

    private void applySymbolHints(Set<String> tags, List<String> relatedSymbols) {
        if (relatedSymbols == null || relatedSymbols.isEmpty()) {
            return;
        }
        for (String raw : relatedSymbols) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String sym = raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
            if (sym.isEmpty()) {
                continue;
            }
            if (CRYPTO_SYMBOL_HINTS.stream().anyMatch(sym::contains)) {
                tags.add("crypto");
            }
            if (FX_SYMBOL_HINTS.stream().anyMatch(h -> sym.contains(h) || sym.endsWith("TRY"))) {
                tags.add("fx");
            }
            if (sym.length() >= 4 && sym.length() <= 6 && !sym.endsWith("USDT")) {
                tags.add("bist");
            }
        }
    }

    private void applyCrossMarketRules(Set<String> tags, String haystack, NewsCategory primary) {
        boolean macroContext = tags.contains("macro")
                || containsAny(haystack, List.of(
                "fed", "ecb", "merkez bankası", "tcmb", "faiz", "enflasyon", "cpi", "pmi",
                "resesyon", "gsyh", "savaş", "jeopolitik", "orta doğu", "petrol", "ham petrol",
                "energy", "oil price", "rate cut", "rate hike", "politika faizi"
        ));
        boolean commodityFx = containsAny(haystack, List.of(
                "altın", "gold", "gümüş", "silver", "petrol", "oil", "doğalgaz", "gas", "emtia",
                "commodity", "ons altın", "xau", "hammadde"
        ));
        boolean turkeyScope = containsAny(haystack, List.of(
                "bist", "borsa istanbul", "türkiye", "turkey", "tcmb", "türk lirası", "tl ", " tl",
                "hisse senedi", "istanbul"
        ));

        if (macroContext) {
            tags.add("macro");
            tags.add("fx");
        }
        if (commodityFx) {
            tags.add("fx");
            tags.add("macro");
        }
        if (turkeyScope || primary == NewsCategory.STOCK) {
            tags.add("bist");
        }
        if (macroContext && (turkeyScope || primary == NewsCategory.STOCK || tags.contains("bist"))) {
            tags.add("bist");
            tags.add("macro");
            tags.add("fx");
        }
        if (primary == NewsCategory.GENERAL_ECONOMY) {
            tags.add("macro");
            tags.add("fx");
        }
    }

    private static boolean containsAny(String haystack, List<String> keywords) {
        if (haystack == null || haystack.isBlank() || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            if (haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeHaystack(String title, String summary) {
        String combined = nz(title) + " " + nz(summary);
        return combined.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
