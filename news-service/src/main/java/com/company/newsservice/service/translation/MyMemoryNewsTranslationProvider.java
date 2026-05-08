package com.company.newsservice.service.translation;

import com.company.newsservice.config.NewsProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

@Component
public class MyMemoryNewsTranslationProvider implements NewsTranslationProvider {

    /**
     * MyMemory uses GET; huge RSS summaries (HTML + data-URI images) blow URL limits and yield no translation.
     */
    private static final int MAX_QUERY_CHARS = 1800;

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
        String forApi = prepareForMyMemory(text);
        if (forApi.isBlank()) {
            return text;
        }
        String sourceLanguage = detectSourceLanguage(forApi);
        String normalizedTarget = normalizeLanguage(targetLanguage);
        if (sourceLanguage.equals(normalizedTarget)) {
            return text;
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(cfg.getBaseUrl())
                .path("/get")
                .queryParam("q", forApi)
                .queryParam("langpair", sourceLanguage + "|" + normalizedTarget)
                .queryParamIfPresent("de", optionalEmail(cfg.getEmail()))
                .build()
                .encode()
                .toUri();

        Map<String, Object> body;
        try {
            body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception ex) {
            return text;
        }

        if (body == null) {
            return text;
        }
        if (!isHttpOkResponseStatus(body.get("responseStatus"))) {
            return text;
        }
        Object quotaFinished = body.get("quotaFinished");
        if (Boolean.TRUE.equals(quotaFinished)) {
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
        if (isMyMemoryLimitOrErrorMessage(translated)) {
            return text;
        }
        return translated;
    }

    private static boolean isHttpOkResponseStatus(Object responseStatus) {
        if (responseStatus == null) {
            return true;
        }
        if (responseStatus instanceof Number n) {
            return n.intValue() == 200;
        }
        return "200".equals(String.valueOf(responseStatus).trim());
    }

    private static boolean isMyMemoryLimitOrErrorMessage(String translated) {
        String upper = translated.toUpperCase(Locale.ROOT);
        return upper.contains("MYMEMORY WARNING") || upper.contains("USAGE LIMIT") || upper.contains("QUOTA");
    }

    private static String prepareForMyMemory(String raw) {
        String s = raw.replaceAll("(?is)<script\\b[^>]*>.*?</script>", " ");
        s = s.replaceAll("<[^>]+>", " ");
        s = s.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&#x27;", "'");
        s = Normalizer.normalize(s, Normalizer.Form.NFC);
        s = s.replace('\u2019', '\'').replace('\u2018', '\'')
                .replace('\u201c', '"').replace('\u201d', '"')
                .replace('\u00a0', ' ');
        s = s.replaceAll("\\s+", " ").trim();
        if (s.length() > MAX_QUERY_CHARS) {
            return s.substring(0, MAX_QUERY_CHARS);
        }
        return s;
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
