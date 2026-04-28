package com.company.newsservice.service;

import com.company.newsservice.config.NewsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NewsInstrumentMatcher {

    private final NewsProperties newsProperties;

    /**
     * Deterministic: iterates {@code news.instrument.keywords} in configured map order;
     * for each symbol, adds at most once when any keyword is a case-insensitive substring of title+summary.
     */
    public List<String> match(String title, String summary) {
        Map<String, List<String>> map = newsProperties.getInstrumentKeywords();
        if (map == null || map.isEmpty()) {
            return List.of();
        }
        String haystack = (nz(title) + " " + nz(summary)).toLowerCase(Locale.ROOT);
        if (haystack.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String symbol = entry.getKey();
            if (symbol == null || symbol.isBlank()) {
                continue;
            }
            List<String> keywords = entry.getValue();
            if (keywords == null) {
                continue;
            }
            for (String raw : keywords) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String kw = raw.toLowerCase(Locale.ROOT);
                if (haystack.contains(kw)) {
                    out.add(symbol);
                    break;
                }
            }
        }
        return out;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
