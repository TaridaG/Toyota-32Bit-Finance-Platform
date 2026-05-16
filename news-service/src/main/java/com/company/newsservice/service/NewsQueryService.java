package com.company.newsservice.service;

import com.company.newsservice.domain.enums.NewsCategory;
import com.company.newsservice.dto.NewsDetailResponse;
import com.company.newsservice.dto.NewsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface NewsQueryService {
    Page<NewsResponse> search(NewsCategory category, String q, Pageable pageable, String language, boolean includeOriginal);
    NewsDetailResponse getById(Long id, String language, boolean includeOriginal);
    List<NewsResponse> listForChart(Instant fromInclusive, Instant toInclusive, String language);
}