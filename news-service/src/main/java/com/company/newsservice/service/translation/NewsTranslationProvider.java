package com.company.newsservice.service.translation;

public interface NewsTranslationProvider {

    String providerId();

    String translate(String text, String targetLanguage);
}
