package com.company.newsservice.service;

import com.company.newsservice.domain.enums.NewsCategory;
import com.company.newsservice.dto.NewsDetailResponse;
import com.company.newsservice.dto.NewsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NewsQueryService {
    Page<NewsResponse> search(NewsCategory category, String q, Pageable pageable);
    NewsDetailResponse getById(Long id);
}