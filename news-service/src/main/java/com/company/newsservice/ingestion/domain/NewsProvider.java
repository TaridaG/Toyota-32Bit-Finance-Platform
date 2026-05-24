package com.company.newsservice.ingestion.domain;

import java.util.List;

/**
 * Dış haber kaynaklarından (RSS vb.) makale listesi sağlayan provider sözleşmesi.
 */
public interface NewsProvider {

    /**
     * Provider tip tanımını döner (ör. {@code RSS}).
     *
     * @return provider tipi
     */
    String providerType();

    /**
     * Kaynaklardan en güncel haber öğelerini çeker.
     *
     * @return normalize edilmiş haber öğeleri
     */
    List<ProviderNewsItem> fetchLatest();
}