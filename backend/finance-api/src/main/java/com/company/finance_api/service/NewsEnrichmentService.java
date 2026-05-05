package com.company.finance_api.service;

import com.company.finance_api.dto.NewsEnrichedPageResponse;
import com.company.finance_api.dto.NewsOriginalResponse;

public interface NewsEnrichmentService {
    NewsEnrichedPageResponse getEnrichedNews(int page, int size, String language);
    NewsOriginalResponse getOriginalNews(Long id);
}
