package com.company.newsservice.image.infrastructure.scheduler;

import com.company.newsservice.bootstrap.config.NewsProperties;
import com.company.newsservice.image.application.ResolveArticleImageUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsArticleImageBackfillSchedulerTest {

    @Mock
    private NewsProperties newsProperties;
    @Mock
    private ResolveArticleImageUseCase resolveArticleImageUseCase;
    @Mock
    private NewsProperties.Image image;

    @InjectMocks
    private NewsArticleImageBackfillScheduler scheduler;

    @Test
    void runBackfill_skipsWhenDisabled() {
        when(newsProperties.getImage()).thenReturn(image);
        when(image.isEnabled()).thenReturn(false);

        scheduler.runBackfill();

        verify(resolveArticleImageUseCase, never()).backfillMissingImages(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void runBackfill_delegatesWhenEnabled() {
        when(newsProperties.getImage()).thenReturn(image);
        when(image.isEnabled()).thenReturn(true);
        when(image.isBackfillEnabled()).thenReturn(true);
        when(image.getBackfillBatchSize()).thenReturn(10);
        when(resolveArticleImageUseCase.backfillMissingImages(10)).thenReturn(4);

        scheduler.runBackfill();

        verify(resolveArticleImageUseCase).backfillMissingImages(10);
    }
}
