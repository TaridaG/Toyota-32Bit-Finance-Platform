package com.company.newsservice.ingestion.application;

import com.company.newsservice.query.domain.NewsArticle;
import com.company.newsservice.query.domain.enums.NewsCategory;
import com.company.newsservice.ingestion.domain.NewsProvider;
import com.company.newsservice.ingestion.domain.ProviderNewsItem;
import com.company.newsservice.query.infrastructure.persistence.NewsArticleRepository;
import com.company.newsservice.ingestion.infrastructure.matching.NewsInstrumentMatcher;
import com.company.newsservice.ingestion.infrastructure.relevance.NewsRelevanceEvaluator;
import com.company.newsservice.ingestion.infrastructure.matching.NewsTopicTagger;
import com.company.newsservice.image.application.ResolveArticleImageUseCase;
import com.company.newsservice.translation.application.TranslateNewsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.company.newsservice.ingestion.infrastructure.kafka.NewsInstrumentMatchedEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class IngestLatestNewsUseCaseTest {

    @Mock
    private NewsArticleRepository newsArticleRepository;

    @Mock
    private NewsProvider newsProvider;

    @Mock
    private NewsInstrumentMatcher newsInstrumentMatcher;

    @Mock
    private NewsRelevanceEvaluator newsRelevanceEvaluator;

    @Mock
    private NewsInstrumentMatchedEventPublisher newsInstrumentMatchedEventPublisher;

    @Mock
    private TranslateNewsUseCase newsTranslationService;

    @Mock
    private NewsTopicTagger newsTopicTagger;

    @Mock
    private ResolveArticleImageUseCase newsArticleImageService;

    private IngestLatestNewsUseCase ingestionService;

    @BeforeEach
    void setUp() {
        lenient().when(newsInstrumentMatcher.match(anyString(), any())).thenReturn(List.of());
        lenient().when(newsRelevanceEvaluator.isRelevant(any())).thenReturn(true);
        lenient().when(newsTopicTagger.resolve(any(), anyString(), any(), any())).thenReturn(List.of("macro"));
        ingestionService =
                new IngestLatestNewsUseCase(
                        List.of(newsProvider),
                        newsArticleRepository,
                        newsInstrumentMatcher,
                        newsRelevanceEvaluator,
                        newsTopicTagger,
                        newsTranslationService,
                        newsArticleImageService,
                        newsInstrumentMatchedEventPublisher
                );
    }

    @Test
    void blankTitleItemIsNotSaved() {
        when(newsProvider.fetchLatest()).thenReturn(List.of(
                new ProviderNewsItem(
                        null,
                        "   ",
                        "summary",
                        "https://example.com/a",
                        "src",
                        NewsCategory.OTHER,
                        Instant.now()
                )
        ));

        assertEquals(0, ingestionService.ingestLatest());

        verify(newsArticleRepository, never()).save(any());
    }

    @Test
    void duplicateUrlPrecheckDoesNotSave() {
        when(newsProvider.fetchLatest()).thenReturn(List.of(
                new ProviderNewsItem(
                        null,
                        "Title",
                        "summary",
                        "https://example.com/dup",
                        "src",
                        NewsCategory.OTHER,
                        Instant.now()
                )
        ));
        when(newsArticleRepository.findByArticleUrl("https://example.com/dup"))
                .thenReturn(Optional.of(new NewsArticle()));

        assertEquals(0, ingestionService.ingestLatest());

        verify(newsArticleRepository, never()).save(any());
    }

    @Test
    void oneItemFailureDoesNotStopOtherItems() {
        ProviderNewsItem first = new ProviderNewsItem(
                null,
                "T1",
                "s1",
                "https://example.com/one",
                "src",
                NewsCategory.OTHER,
                Instant.now()
        );
        ProviderNewsItem second = new ProviderNewsItem(
                null,
                "T2",
                "s2",
                "https://example.com/two",
                "src",
                NewsCategory.OTHER,
                Instant.now()
        );
        when(newsProvider.fetchLatest()).thenReturn(List.of(first, second));
        when(newsArticleRepository.findByArticleUrl(anyString())).thenReturn(Optional.empty());
        when(newsArticleRepository.save(any(NewsArticle.class)))
                .thenThrow(new RuntimeException("simulated failure"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(1, ingestionService.ingestLatest());

        verify(newsArticleRepository, times(2)).save(any());
    }

    @Test
    void matchedSymbolsPublishKafkaEvent() {
        Instant publishedAt = Instant.parse("2026-05-23T09:00:00Z");
        ProviderNewsItem item = new ProviderNewsItem(
                null,
                "Bitcoin hits new high",
                "summary",
                "https://example.com/btc",
                "Reuters",
                NewsCategory.CRYPTO,
                publishedAt
        );
        when(newsProvider.fetchLatest()).thenReturn(List.of(item));
        when(newsArticleRepository.findByArticleUrl("https://example.com/btc")).thenReturn(Optional.empty());
        when(newsInstrumentMatcher.match("Bitcoin hits new high", "summary")).thenReturn(List.of("BTCUSDT"));
        when(newsArticleRepository.save(any(NewsArticle.class))).thenAnswer(invocation -> {
            NewsArticle saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        assertEquals(1, ingestionService.ingestLatest());

        verify(newsInstrumentMatchedEventPublisher).publish(
                "https://example.com/btc",
                List.of("BTCUSDT"),
                "Bitcoin hits new high",
                "Reuters",
                publishedAt
        );
    }
}
