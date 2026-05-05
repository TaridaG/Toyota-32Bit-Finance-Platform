package com.company.newsservice.service;

import com.company.newsservice.config.NewsProperties;
import com.company.newsservice.provider.ProviderNewsItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NewsRelevanceEvaluator {

    private static final int SCORE_GLOBAL_KEYWORD = 1;
    private static final int SCORE_CATEGORY_KEYWORD = 2;
    private static final int SCORE_INSTRUMENT_KEYWORD = 2;
    private static final int MAX_GLOBAL_KEYWORD_SCORE = 4;
    private static final int MAX_INSTRUMENT_KEYWORD_SCORE = 6;

    private final NewsProperties newsProperties;

    public boolean isRelevant(ProviderNewsItem item) {
        NewsProperties.Relevance relevance = newsProperties.getRelevance();
        if (relevance == null || !relevance.isEnabled()) {
            return true;
        }

        String haystack = normalize(item.title(), item.summary());
        if (haystack.isBlank()) {
            return false;
        }

        if (containsAny(haystack, relevance.safeBlockedKeywords())) {
            return false;
        }

        String categoryName = item.category() == null ? "" : item.category().name();
        List<String> categoryKeywords = relevance.keywordsForCategory(categoryName);
        int score = 0;

        score += Math.min(MAX_GLOBAL_KEYWORD_SCORE, SCORE_GLOBAL_KEYWORD * countMatches(haystack, relevance.safeGlobalKeywords()));
        score += SCORE_CATEGORY_KEYWORD * countMatches(haystack, categoryKeywords);
        score += Math.min(MAX_INSTRUMENT_KEYWORD_SCORE, SCORE_INSTRUMENT_KEYWORD * countInstrumentMatches(haystack));

        if (relevance.isRequireCategoryKeywordMatch() && !categoryKeywords.isEmpty() && !containsAny(haystack, categoryKeywords)) {
            return false;
        }

        return score >= relevance.getMinScore();
    }

    private int countInstrumentMatches(String haystack) {
        int matchedSymbols = 0;
        Map<String, List<String>> instrumentKeywords = newsProperties.getInstrumentKeywords();
        if (instrumentKeywords == null || instrumentKeywords.isEmpty()) {
            return 0;
        }
        for (Map.Entry<String, List<String>> entry : instrumentKeywords.entrySet()) {
            if (containsAny(haystack, entry.getValue())) {
                matchedSymbols++;
            }
        }
        return matchedSymbols;
    }

    private int countMatches(String haystack, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            if (haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                count++;
            }
        }
        return count;
    }

    private boolean containsAny(String haystack, List<String> keywords) {
        return countMatches(haystack, keywords) > 0;
    }

    private String normalize(String title, String summary) {
        return ((title == null ? "" : title) + " " + (summary == null ? "" : summary))
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
