package com.company.newsservice.ingestion.infrastructure.matching;

import com.company.newsservice.bootstrap.config.NewsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Haber başlık ve özetinde yapılandırılmış enstrüman anahtar kelimelerine göre sembol eşleştirmesi yapar.
 */
@Service
@RequiredArgsConstructor
public class NewsInstrumentMatcher {

    private final NewsProperties newsProperties;

    /**
     * Başlık ve özette yapılandırılmış keyword'lere göre eşleşen enstrüman sembollerini döner.
     * Yapılandırılmış map sırasına göre deterministik çalışır; her sembol en fazla bir kez eklenir.
     *
     * @param title makale başlığı
     * @param summary makale özeti
     * @return eşleşen sembol listesi
     */
    public List<String> match(String title, String summary) {
        Map<String, List<String>> map = newsProperties.getInstrumentKeywords();
        if (map.isEmpty()) {
            return List.of();
        }
        String haystack = normalizeHaystack(title, summary);
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

    private static String normalizeHaystack(String title, String summary) {
        String combined = nz(title) + " " + nz(summary);
        return combined.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
