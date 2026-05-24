package com.company.newsservice.query.application;

import com.company.newsservice.ingestion.infrastructure.matching.NewsTopicTagger;
import com.company.newsservice.query.domain.NewsArticle;
import com.company.newsservice.query.domain.enums.NewsCategory;
import com.company.newsservice.query.infrastructure.http.dto.NewsDetailResponse;
import com.company.newsservice.query.infrastructure.http.dto.NewsResponse;
import com.company.newsservice.query.infrastructure.persistence.NewsArticleRepository;
import com.company.newsservice.query.infrastructure.symbols.NewsRelatedSymbolsResolver;
import com.company.newsservice.shared.web.ResourceNotFoundException;
import com.company.newsservice.translation.application.TranslateNewsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsQueryUseCaseTest {

    @Mock
    private NewsArticleRepository newsArticleRepository;
    @Mock
    private TranslateNewsUseCase newsTranslationService;
    @Mock
    private NewsRelatedSymbolsResolver newsRelatedSymbolsResolver;
    @Mock
    private NewsTopicTagger newsTopicTagger;

    @InjectMocks
    private NewsQueryUseCase useCase;

    @Test
    void search_usesCategoryWhenQueryBlank() {
        NewsArticle article = sampleArticle(1L);
        when(newsArticleRepository.searchByCategory(eq(NewsCategory.STOCK), any()))
                .thenReturn(new PageImpl<>(List.of(article)));
        when(newsRelatedSymbolsResolver.readOrMatch(article)).thenReturn(List.of("THYAO"));
        when(newsTranslationService.resolveBatch(List.of(article), "tr"))
                .thenReturn(Map.of(1L, projection("Translated title")));

        var page = useCase.search(NewsCategory.STOCK, "  ", PageRequest.of(0, 10), "tr", false);

        assertEquals(1, page.getTotalElements());
        assertEquals("Translated title", page.getContent().getFirst().title());
        verify(newsArticleRepository).searchByCategory(NewsCategory.STOCK, PageRequest.of(0, 10));
    }

    @Test
    void getById_throwsWhenArticleMissing() {
        when(newsArticleRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.getById(99L, "en", false));
    }

    @Test
    void getById_returnsDetailWithPersistedSymbols() {
        NewsArticle article = sampleArticle(5L);
        when(newsArticleRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(article));
        when(newsRelatedSymbolsResolver.resolveAndPersist(article)).thenReturn(List.of("BTCUSDT"));
        when(newsTranslationService.resolveBatch(List.of(article), "en"))
                .thenReturn(Map.of(5L, projection("BTC headline")));

        NewsDetailResponse detail = useCase.getById(5L, "en", true);

        assertEquals(5L, detail.id());
        assertEquals(List.of("BTCUSDT"), detail.relatedSymbols());
        assertEquals("BTC headline", detail.title());
    }

    @Test
    void search_usesTextQueryWhenProvided() {
        NewsArticle article = sampleArticle(2L);
        when(newsArticleRepository.searchByQuery(eq("bitcoin"), any()))
                .thenReturn(new PageImpl<>(List.of(article)));
        when(newsRelatedSymbolsResolver.readOrMatch(article)).thenReturn(List.of());
        when(newsTranslationService.resolveBatch(List.of(article), "en"))
                .thenReturn(Map.of(2L, projection("Bitcoin headline")));

        var page = useCase.search(null, "bitcoin", PageRequest.of(0, 10), "en", false);

        assertEquals(1, page.getTotalElements());
        verify(newsArticleRepository).searchByQuery("bitcoin", PageRequest.of(0, 10));
    }

    @Test
    void listForChart_returnsEmptyWhenRangeInvalid() {
        Instant from = Instant.parse("2026-05-23T12:00:00Z");
        Instant to = Instant.parse("2026-05-23T10:00:00Z");

        assertTrue(useCase.listForChart(from, to, "en").isEmpty());
    }

    private static NewsArticle sampleArticle(long id) {
        NewsArticle article = new NewsArticle();
        article.setId(id);
        article.setTitle("Original");
        article.setSummary("Summary");
        article.setArticleUrl("https://example.com/" + id);
        article.setSourceName("Reuters");
        article.setCategory(NewsCategory.STOCK);
        article.setPublishedAt(Instant.parse("2026-05-23T08:00:00Z"));
        article.setTopicTags(List.of("bist"));
        return article;
    }

    private static TranslateNewsUseCase.NewsTextProjection projection(String title) {
        return new TranslateNewsUseCase.NewsTextProjection(
                "Original",
                "Summary",
                title,
                "Translated summary",
                "tr",
                true
        );
    }
}
