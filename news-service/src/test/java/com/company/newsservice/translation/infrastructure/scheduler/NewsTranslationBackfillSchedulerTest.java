package com.company.newsservice.translation.infrastructure.scheduler;

import com.company.newsservice.bootstrap.config.NewsProperties;
import com.company.newsservice.translation.application.TranslateNewsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsTranslationBackfillSchedulerTest {

    @Mock
    private NewsProperties newsProperties;
    @Mock
    private TranslateNewsUseCase translateNewsUseCase;
    @Mock
    private NewsProperties.Translation translation;

    @InjectMocks
    private NewsTranslationBackfillScheduler scheduler;

    @Test
    void runBackfill_skipsWhenDisabled() {
        when(newsProperties.getTranslation()).thenReturn(translation);
        when(translation.isEnabled()).thenReturn(false);

        scheduler.runBackfill();

        verify(translateNewsUseCase, never()).backfillMissingTranslations(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void runBackfill_delegatesWhenEnabled() {
        when(newsProperties.getTranslation()).thenReturn(translation);
        when(translation.isEnabled()).thenReturn(true);
        when(translation.isBackfillEnabled()).thenReturn(true);
        when(translation.getBackfillBatchSize()).thenReturn(25);
        when(translateNewsUseCase.backfillMissingTranslations(25)).thenReturn(2);

        scheduler.runBackfill();

        verify(translateNewsUseCase).backfillMissingTranslations(25);
    }
}
