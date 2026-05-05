package com.company.newsservice.service.translation;

import org.springframework.stereotype.Component;

@Component
public class NoopNewsTranslationProvider implements NewsTranslationProvider {

    @Override
    public String providerId() {
        return "noop";
    }

    @Override
    public String translate(String text, String targetLanguage) {
        return text == null ? "" : text;
    }
}
