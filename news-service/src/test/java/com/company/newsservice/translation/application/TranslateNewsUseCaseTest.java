package com.company.newsservice.translation.application;

import com.company.newsservice.bootstrap.config.NewsProperties;
import com.company.newsservice.query.domain.NewsArticle;
import com.company.newsservice.query.infrastructure.persistence.NewsArticleRepository;
import com.company.newsservice.translation.infrastructure.persistence.NewsArticleTranslationRepository;
import com.company.newsservice.translation.domain.NewsArticleTranslation;
import com.company.newsservice.translation.infrastructure.provider.NoopNewsTranslationProvider;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslateNewsUseCaseTest {

    @Mock
    private NewsArticleRepository newsArticleRepository;
    @Mock
    private NewsArticleTranslationRepository translationRepository;

    private NewsProperties newsProperties;
    private TranslateNewsUseCase useCase;

    @BeforeEach
    void setUp() {
        newsProperties = new NewsProperties();
        newsProperties.getTranslation().setEnabled(false);
        newsProperties.getTranslation().setDefaultLanguage("en");
        newsProperties.getTranslation().setSupportedLanguages(List.of("en", "tr"));

        useCase = new TranslateNewsUseCase(
                newsProperties,
                newsArticleRepository,
                translationRepository,
                List.of(new NoopNewsTranslationProvider()),
                new SimpleMeterRegistry()
        );
    }

    @Test
    void resolveRequestedLanguage_fallsBackToDefaultWhenUnsupported() {
        assertEquals("en", useCase.resolveRequestedLanguage("de"));
    }

    @Test
    void resolveRequestedLanguage_normalizesLocaleTag() {
        assertEquals("tr", useCase.resolveRequestedLanguage("tr-TR"));
    }

    @Test
    void resolveBatch_returnsOriginalTextWhenTranslationDisabled() {
        NewsArticle article = new NewsArticle();
        article.setId(10L);
        article.setTitle("Headline");
        article.setSummary("Body");

        Map<Long, TranslateNewsUseCase.NewsTextProjection> batch = useCase.resolveBatch(List.of(article), "tr");

        TranslateNewsUseCase.NewsTextProjection projection = batch.get(10L);
        assertEquals("Headline", projection.titleTranslated());
        assertEquals("Body", projection.summaryTranslated());
        assertFalse(projection.translated());
    }

    @Test
    void supportedLanguages_returnsConfiguredList() {
        assertEquals(List.of("en", "tr"), useCase.supportedLanguages());
    }

    @Test
    void supportedLanguages_usesDefaultWhenListEmpty() {
        newsProperties.getTranslation().setSupportedLanguages(List.of());

        assertTrue(useCase.supportedLanguages().contains("en"));
    }

    @Test
    void resolveBatch_returnsCachedTranslationWhenEnabled() {
        newsProperties.getTranslation().setEnabled(true);
        newsProperties.getTranslation().setProvider("noop");

        NewsArticle article = new NewsArticle();
        article.setId(20L);
        article.setTitle("Original title");
        article.setSummary("Original summary");

        NewsArticleTranslation cached = new NewsArticleTranslation();
        cached.setNewsArticle(article);
        cached.setLanguageCode("tr");
        cached.setTitleTranslated("Cached title");
        cached.setSummaryTranslated("Cached summary");

        when(translationRepository.findByNewsArticleIdInAndLanguageCode(List.of(20L), "tr"))
                .thenReturn(List.of(cached));

        Map<Long, TranslateNewsUseCase.NewsTextProjection> batch =
                useCase.resolveBatch(List.of(article), "tr");

        TranslateNewsUseCase.NewsTextProjection projection = batch.get(20L);
        assertEquals("Cached title", projection.titleTranslated());
        assertEquals("Cached summary", projection.summaryTranslated());
        assertTrue(projection.translated());
        verify(translationRepository).findByNewsArticleIdInAndLanguageCode(List.of(20L), "tr");
    }

    @Test
    void backfillMissingTranslations_returnsZeroWhenDisabled() {
        assertEquals(0, useCase.backfillMissingTranslations(50));
    }

    @Test
    void backfillMissingTranslations_processesMissingRowsWhenEnabled() {
        newsProperties.getTranslation().setEnabled(true);
        newsProperties.getTranslation().setProvider("noop");
        newsProperties.getTranslation().setSupportedLanguages(List.of("en"));

        NewsArticle article = new NewsArticle();
        article.setId(30L);
        article.setTitle("Title");
        article.setSummary("Summary");

        when(newsArticleRepository.findActiveWithoutTranslation(eq("en"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(article)));
        when(translationRepository.save(org.mockito.ArgumentMatchers.any(NewsArticleTranslation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(1, useCase.backfillMissingTranslations(10));
    }
}
