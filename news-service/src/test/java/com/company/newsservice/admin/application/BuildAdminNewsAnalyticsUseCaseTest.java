package com.company.newsservice.admin.application;

import com.company.newsservice.admin.domain.AdminNewsAnalyticsPreset;
import com.company.newsservice.admin.infrastructure.http.dto.AdminNewsAnalyticsDashboardDto;
import com.company.newsservice.query.domain.NewsArticle;
import com.company.newsservice.query.domain.enums.NewsCategory;
import com.company.newsservice.query.infrastructure.persistence.NewsArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildAdminNewsAnalyticsUseCaseTest {

    @Mock
    private NewsArticleRepository newsArticleRepository;

    private BuildAdminNewsAnalyticsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BuildAdminNewsAnalyticsUseCase(newsArticleRepository, "en");
        lenient().when(newsArticleRepository.countByActiveTrue()).thenReturn(100L);
        lenient().when(newsArticleRepository.countDistinctSourceNameByActiveTrue()).thenReturn(5L);
        lenient().when(newsArticleRepository.countActiveWithTranslationLanguage("en")).thenReturn(80L);
        lenient().when(newsArticleRepository.countPublishedBetween(any(), any())).thenReturn(4L);
        lenient().when(newsArticleRepository.countCreatedBetweenActiveTrue(any(), any())).thenReturn(2L);
        lenient().when(newsArticleRepository.findByActiveTrueOrderByPublishedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(sampleArticle()));
    }

    @Test
    void dashboard_buildsSevenDaySeriesForPreset() {
        AdminNewsAnalyticsDashboardDto dashboard = useCase.dashboard(AdminNewsAnalyticsPreset.LAST_7_DAYS);

        assertEquals("7d", dashboard.preset());
        assertEquals(7, dashboard.dailyPublished().size());
        assertEquals(100L, dashboard.summary().totalArticles());
        assertEquals(5, dashboard.summary().distinctSourceCount());
        assertEquals(80.0, dashboard.summary().translationCompletionPercent());
        assertEquals(1, dashboard.recentArticles().size());
    }

    @Test
    void dashboardCustom_buildsCustomRange() {
        LocalDate from = LocalDate.now(ZoneOffset.UTC).minusDays(3);
        LocalDate to = LocalDate.now(ZoneOffset.UTC);

        AdminNewsAnalyticsDashboardDto dashboard = useCase.dashboardCustom(from, to);

        assertEquals("custom", dashboard.preset());
        assertEquals(4, dashboard.dailyPublished().size());
    }

    @Test
    void dashboardCustom_requiresBothDates() {
        assertThrows(IllegalArgumentException.class, () -> useCase.dashboardCustom(null, LocalDate.now(ZoneOffset.UTC)));
        assertThrows(IllegalArgumentException.class, () -> useCase.dashboardCustom(LocalDate.now(ZoneOffset.UTC), null));
    }

    @Test
    void dashboardCustom_rejectsInvertedRange() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.dashboardCustom(today, today.minusDays(1))
        );
    }

    @Test
    void dashboardCustom_rejectsRangeLongerThanMaxDays() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = today.minusDays(BuildAdminNewsAnalyticsUseCase.MAX_CUSTOM_RANGE_DAYS);

        assertThrows(IllegalArgumentException.class, () -> useCase.dashboardCustom(from, today));
    }

    private static NewsArticle sampleArticle() {
        NewsArticle article = new NewsArticle();
        article.setId(1L);
        article.setTitle("Headline");
        article.setSourceName("Reuters");
        article.setCategory(NewsCategory.STOCK);
        article.setPublishedAt(Instant.parse("2026-05-23T08:00:00Z"));
        return article;
    }
}
