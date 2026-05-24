package com.company.newsservice.admin.application;

import com.company.newsservice.admin.infrastructure.http.dto.AdminNewsDashboardMetricsDto;
import com.company.newsservice.query.infrastructure.persistence.NewsArticleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin news dashboard için hafif metrik snapshot'ı döner.
 */
@Component
public class GetAdminNewsDashboardUseCase {

    private final NewsArticleRepository newsArticleRepository;

    public GetAdminNewsDashboardUseCase(NewsArticleRepository newsArticleRepository) {
        this.newsArticleRepository = newsArticleRepository;
    }

    /** Toplam article, distinct source ve son 7 UTC gün publish sayılarını toplar. */
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
