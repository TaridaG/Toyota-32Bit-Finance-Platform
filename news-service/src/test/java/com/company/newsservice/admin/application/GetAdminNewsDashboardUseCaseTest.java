package com.company.newsservice.admin.application;

import com.company.newsservice.admin.infrastructure.http.dto.AdminNewsDashboardMetricsDto;
import com.company.newsservice.query.infrastructure.persistence.NewsArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAdminNewsDashboardUseCaseTest {

    @Mock
    private NewsArticleRepository newsArticleRepository;

    @InjectMocks
    private GetAdminNewsDashboardUseCase useCase;

    @Test
    void snapshot_aggregatesTotalsAndDailySeries() {
        when(newsArticleRepository.countByActiveTrue()).thenReturn(120L);
        when(newsArticleRepository.countDistinctSourceNameByActiveTrue()).thenReturn(8L);
        when(newsArticleRepository.countPublishedBetween(any(), any())).thenReturn(3L, 5L, 2L, 7L, 1L, 4L, 6L);

        AdminNewsDashboardMetricsDto metrics = useCase.snapshot();

        assertEquals(120L, metrics.totalArticles());
        assertEquals(8, metrics.distinctSourceCount());
        assertEquals(7, metrics.articlesPublishedDailyLast7Utc().size());
        assertEquals(6, metrics.articlesPublishedDailyLast7Utc().getLast());
        verify(newsArticleRepository, times(7)).countPublishedBetween(any(), any());
    }

    @Test
    void snapshot_returnsSevenDailyBuckets() {
        when(newsArticleRepository.countByActiveTrue()).thenReturn(1L);
        when(newsArticleRepository.countDistinctSourceNameByActiveTrue()).thenReturn(1L);
        when(newsArticleRepository.countPublishedBetween(any(), any())).thenReturn(0L);

        AdminNewsDashboardMetricsDto metrics = useCase.snapshot();

        assertEquals(7, metrics.articlesPublishedDailyLast7Utc().size());
    }
}
