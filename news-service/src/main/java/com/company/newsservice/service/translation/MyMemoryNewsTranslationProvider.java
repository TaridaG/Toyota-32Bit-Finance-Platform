package com.company.newsservice.service.translation;

import com.company.newsservice.config.NewsProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

@Component
public class MyMemoryNewsTranslationProvider implements NewsTranslationProvider {

    private final NewsProperties newsProperties;
    private final RestClient restClient = RestClient.create();

    public MyMemoryNewsTranslationProvider(NewsProperties newsProperties) {
        this.newsProperties = newsProperties;
    }

    @Override
    public String providerId() {
        return "mymemory";
    }

    @Override
    @SuppressWarnings("unchecked")
    public String translate(String text, String targetLanguage) {
        if (text == null || text.isBlank()) {
            return "";
        }

        NewsProperties.Mymemory cfg = newsProperties.getTranslation().getMymemory();
        String sourceLanguage = detectSourceLanguage(text);
        String normalizedTarget = normalizeLanguage(targetLanguage);
        if (sourceLanguage.equals(normalizedTarget)) {
            return text;
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(cfg.getBaseUrl())
                .path("/get")
                .queryParam("q", text)
                .queryParam("langpair", sourceLanguage + "|" + normalizedTarget)
                .queryParamIfPresent("de", optionalEmail(cfg.getEmail()))
                .build()
                .encode()
                .toUri();

        Map<String, Object> body = restClient.get()
                .uri(uri)
                .retrieve()
                .body(Map.class);

        if (body == null) {
            return text;
        }
        Object responseData = body.get("responseData");
        if (!(responseData instanceof Map<?, ?> responseMap)) {
            return text;
        }
        Object translatedText = responseMap.get("translatedText");
        if (!(translatedText instanceof String translated) || translated.isBlank()) {
            return text;
        }
        return translated;
    }

    private java.util.Optional<String> optionalEmail(String email) {
        if (email == null || email.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(email.trim());
    }

    private String detectSourceLanguage(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("ğ") || lower.contains("ü") || lower.contains("ş")
                || lower.contains("ı") || lower.contains("ö") || lower.contains("ç")) {
            return "tr";
        }
        return "en";
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "en";
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("-")) {
            normalized = normalized.split("-")[0];
        }
        if (normalized.contains(",")) {
            normalized = normalized.split(",")[0].trim();
        }
        return normalized.isBlank() ? "en" : normalized;
    }
}
