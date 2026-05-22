package com.company.newsservice.service.impl;

import com.company.newsservice.domain.NewsArticle;
import com.company.newsservice.domain.enums.NewsCategory;
import com.company.newsservice.provider.NewsProvider;
import com.company.newsservice.provider.ProviderNewsItem;
import com.company.newsservice.repository.NewsArticleRepository;
import com.company.newsservice.service.NewsInstrumentMatcher;
import com.company.newsservice.service.NewsRelevanceEvaluator;
import com.company.newsservice.service.NewsTopicTagger;
import com.company.newsservice.image.NewsArticleImageService;
import com.company.newsservice.service.translation.NewsTranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

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
class NewsIngestionServiceImplTest {

    @Mock
    private NewsArticleRepository newsArticleRepository;

    @Mock
    private NewsProvider newsProvider;

    @Mock
    private NewsInstrumentMatcher newsInstrumentMatcher;

    @Mock
    private NewsRelevanceEvaluator newsRelevanceEvaluator;

    @Mock
    private KafkaTemplate<String, Object> newsKafkaTemplate;

    @Mock
    private NewsTranslationService newsTranslationService;

    @Mock
    private NewsTopicTagger newsTopicTagger;

    @Mock
    private NewsArticleImageService newsArticleImageService;

    private NewsIngestionServiceImpl ingestionService;

    @BeforeEach
    void setUp() {
        lenient().when(newsInstrumentMatcher.match(anyString(), any())).thenReturn(List.of());
        lenient().when(newsRelevanceEvaluator.isRelevant(any())).thenReturn(true);
        lenient().when(newsTopicTagger.resolve(any(), anyString(), any(), any())).thenReturn(List.of("macro"));
        ingestionService =
                new NewsIngestionServiceImpl(
                        List.of(newsProvider),
                        newsArticleRepository,
                        newsInstrumentMatcher,
                        newsRelevanceEvaluator,
                        newsTopicTagger,
                        newsTranslationService,
                        newsArticleImageService,
                        newsKafkaTemplate
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
}
