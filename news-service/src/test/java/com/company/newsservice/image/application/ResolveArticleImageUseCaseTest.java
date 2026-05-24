package com.company.newsservice.image.application;

import com.company.newsservice.bootstrap.config.NewsProperties;
import com.company.newsservice.image.infrastructure.async.NewsArticleImageResolveEvent;
import com.company.newsservice.image.infrastructure.http.ArticlePageImageFetcher;
import com.company.newsservice.image.infrastructure.rss.RssDescriptionImageExtractor;
import com.company.newsservice.query.domain.NewsArticle;
import com.company.newsservice.query.infrastructure.persistence.NewsArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveArticleImageUseCaseTest {

    @Mock
    private NewsArticleRepository newsArticleRepository;
    @Mock
    private RssDescriptionImageExtractor rssDescriptionImageExtractor;
    @Mock
    private ArticlePageImageFetcher articlePageImageFetcher;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private NewsProperties newsProperties;

    @InjectMocks
    private ResolveArticleImageUseCase useCase;

    @BeforeEach
    void setUp() {
        newsProperties = new NewsProperties();
        newsProperties.getImage().setEnabled(true);
        useCase = new ResolveArticleImageUseCase(
                newsProperties,
                newsArticleRepository,
                rssDescriptionImageExtractor,
                articlePageImageFetcher,
                applicationEventPublisher
        );
    }

    @Test
    void scheduleResolveAfterIngest_publishesEventWhenEnabled() {
        useCase.scheduleResolveAfterIngest(42L, "<img src='/a.jpg'/>");

        ArgumentCaptor<NewsArticleImageResolveEvent> captor = ArgumentCaptor.forClass(NewsArticleImageResolveEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertEqualsEvent(42L, captor.getValue());
    }

    @Test
    void resolveForArticle_usesRssImageBeforePageFetch() {
        NewsArticle article = articleWithoutImage(7L);
        when(newsArticleRepository.findById(7L)).thenReturn(Optional.of(article));
        when(rssDescriptionImageExtractor.extract("<img/>", article.getArticleUrl()))
                .thenReturn("https://cdn.example.com/a.jpg");

        assertTrue(useCase.resolveForArticle(7L, "<img/>"));

        verify(articlePageImageFetcher, never()).fetch(any());
        verify(newsArticleRepository).save(article);
        assertEquals("https://cdn.example.com/a.jpg", article.getImageUrl());
    }

    @Test
    void resolveForArticle_returnsFalseWhenArticleAlreadyHasImage() {
        NewsArticle article = articleWithoutImage(8L);
        article.setImageUrl("https://cdn.example.com/existing.jpg");
        when(newsArticleRepository.findById(8L)).thenReturn(Optional.of(article));

        assertFalse(useCase.resolveForArticle(8L, "hint"));

        verify(rssDescriptionImageExtractor, never()).extract(any(), any());
    }

    private static NewsArticle articleWithoutImage(long id) {
        NewsArticle article = new NewsArticle();
        article.setId(id);
        article.setArticleUrl("https://example.com/" + id);
        article.setSummary("summary");
        return article;
    }

    private static void assertEqualsEvent(long articleId, NewsArticleImageResolveEvent event) {
        assertEquals(articleId, event.articleId());
    }
}
