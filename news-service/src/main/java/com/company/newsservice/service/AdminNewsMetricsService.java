package com.company.newsservice.service;

import com.company.newsservice.dto.AdminNewsDashboardMetricsDto;
import com.company.newsservice.repository.NewsArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminNewsMetricsService {

    private final NewsArticleRepository newsArticleRepository;

    public AdminNewsMetricsService(NewsArticleRepository newsArticleRepository) {
        this.newsArticleRepository = newsArticleRepository;
    }

    @Transactional(readOnly = true)
    public AdminNewsDashboardMetricsDto snapshot() {
        Instant now = Instant.now();
        long total = newsArticleRepository.countByActiveTrue();
        int sources = (int) Math.min(Integer.MAX_VALUE, newsArticleRepository.countDistinctSourceNameByActiveTrue());
        List<Integer> daily = dailyArticlesPublishedUtcLast7Days(now);
        return new AdminNewsDashboardMetricsDto(total, sources, daily, now);
    }

    private List<Integer> dailyArticlesPublishedUtcLast7Days(Instant now) {
        LocalDate todayUtc = LocalDate.ofInstant(now, ZoneOffset.UTC);
        List<Integer> out = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            LocalDate d = todayUtc.minusDays(i);
            Instant start = d.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = start.plus(1, ChronoUnit.DAYS);
            out.add((int) Math.min(Integer.MAX_VALUE, newsArticleRepository.countPublishedBetween(start, end)));
        }
        return out;
    }
}
