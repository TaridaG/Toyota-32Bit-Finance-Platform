package com.company.newsservice.provider;

import java.util.List;

public interface NewsProvider {
    String providerType();
    List<ProviderNewsItem> fetchLatest();
}