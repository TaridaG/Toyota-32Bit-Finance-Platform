package com.company.newsservice.translation.infrastructure.provider;

import org.springframework.stereotype.Component;

/**
 * Çeviri yapmadan kaynak metni döndüren test/dev provider implementasyonu.
 */
@Component
public class NoopNewsTranslationProvider implements NewsTranslationProvider {

    /** {@inheritDoc} */
    public String providerId() {
        return "noop";
    }

    /** {@inheritDoc} */
    public String translate(String text, String targetLanguage) {
        return text == null ? "" : text;
    }
}
