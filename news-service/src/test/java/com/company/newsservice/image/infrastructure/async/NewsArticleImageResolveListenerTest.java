package com.company.newsservice.image.infrastructure.async;

import com.company.newsservice.image.application.ResolveArticleImageUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NewsArticleImageResolveListenerTest {

    @Mock
    private ResolveArticleImageUseCase resolveArticleImageUseCase;

    @InjectMocks
    private NewsArticleImageResolveListener listener;

    @Test
    void onArticleIngested_delegatesToImageUseCase() {
        NewsArticleImageResolveEvent event = new NewsArticleImageResolveEvent(55L, "<img src='/a.jpg'/>");

        listener.onArticleIngested(event);

        verify(resolveArticleImageUseCase).resolveForArticle(55L, "<img src='/a.jpg'/>");
    }

    @Test
    void onArticleIngested_swallowsResolveFailure() {
        NewsArticleImageResolveEvent event = new NewsArticleImageResolveEvent(77L, "summary");
        doThrow(new IllegalStateException("fetch failed"))
                .when(resolveArticleImageUseCase)
                .resolveForArticle(77L, "summary");

        listener.onArticleIngested(event);

        verify(resolveArticleImageUseCase).resolveForArticle(77L, "summary");
    }
}
