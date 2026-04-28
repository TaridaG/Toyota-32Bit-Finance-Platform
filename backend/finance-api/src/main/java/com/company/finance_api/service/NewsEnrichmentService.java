package com.company.finance_api.service;

import com.company.finance_api.dto.NewsEnrichedPageResponse;

public interface NewsEnrichmentService {
    NewsEnrichedPageResponse getEnrichedNews(int page, int size);
}
