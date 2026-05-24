package com.company.newsservice.ingestion.infrastructure.scheduler;

import com.company.newsservice.bootstrap.config.NewsProperties;
import com.company.newsservice.ingestion.application.IngestLatestNewsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsIngestionSchedulerTest {

    @Mock
    private NewsProperties newsProperties;
    @Mock
    private IngestLatestNewsUseCase ingestLatestNewsUseCase;
    @Mock
    private NewsProperties.Scheduler scheduler;

    @InjectMocks
    private NewsIngestionScheduler ingestionScheduler;

    @Test
    void ingest_skipsWhenSchedulerDisabled() {
        when(newsProperties.getScheduler()).thenReturn(scheduler);
        when(scheduler.isEnabled()).thenReturn(false);

        ingestionScheduler.ingest();

        verify(ingestLatestNewsUseCase, never()).ingestLatest();
    }

    @Test
    void ingest_delegatesToUseCaseWhenEnabled() {
        when(newsProperties.getScheduler()).thenReturn(scheduler);
        when(scheduler.isEnabled()).thenReturn(true);
        when(ingestLatestNewsUseCase.ingestLatest()).thenReturn(3);

        ingestionScheduler.ingest();

        verify(ingestLatestNewsUseCase).ingestLatest();
    }
}
