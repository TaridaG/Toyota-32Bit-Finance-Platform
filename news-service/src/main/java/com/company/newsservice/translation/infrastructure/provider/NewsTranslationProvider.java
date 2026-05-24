package com.company.newsservice.translation.infrastructure.provider;

/**
 * Harici veya noop haber çeviri provider sözleşmesi.
 */
public interface NewsTranslationProvider {

    /** Yapılandırmada kullanılan provider kimliğini döner (ör. {@code noop}, {@code mymemory}). */
    String providerId();

    /** Metni hedef dile çevirir; hata durumunda kaynak metni dönebilir. */
    String translate(String text, String targetLanguage);
}
