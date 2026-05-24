package com.company.newsservice.query.infrastructure.http;

import com.company.newsservice.ingestion.application.IngestLatestNewsUseCase;
import com.company.newsservice.query.application.NewsQueryUseCase;
import com.company.newsservice.query.domain.enums.NewsCategory;
import com.company.newsservice.query.infrastructure.http.dto.NewsResponse;
import com.company.newsservice.shared.web.ApiResponse;
import com.company.newsservice.shared.web.GlobalExceptionHandler;
import com.company.newsservice.shared.web.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NewsControllerTest {

    @Mock
    private NewsQueryUseCase newsQueryUseCase;
    @Mock
    private IngestLatestNewsUseCase ingestLatestNewsUseCase;

    private NewsController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new NewsController(newsQueryUseCase, ingestLatestNewsUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void list_delegatesToQueryUseCase() {
        NewsResponse item = sampleResponse();
        when(newsQueryUseCase.search(NewsCategory.STOCK, "btc", PageRequest.of(0, 20), "tr", false))
                .thenReturn(new PageImpl<>(List.of(item)));

        ApiResponse<Page<NewsResponse>> response = controller.list(
                NewsCategory.STOCK,
                "btc",
                "tr",
                false,
                0,
                20
        );

        assertTrue(response.success());
        assertEquals(1, response.data().getTotalElements());
        verify(newsQueryUseCase).search(NewsCategory.STOCK, "btc", PageRequest.of(0, 20), "tr", false);
    }

    @Test
    void detail_returnsNotFoundThroughExceptionHandler() throws Exception {
        when(newsQueryUseCase.getById(99L, "en", true))
                .thenThrow(new ResourceNotFoundException("News article not found: 99"));

        mockMvc.perform(get("/api/news/99").param("lang", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void ingestNow_returnsSavedCount() throws Exception {
        when(ingestLatestNewsUseCase.ingestLatest()).thenReturn(3);

        mockMvc.perform(post("/api/news/admin/ingest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));

        verify(ingestLatestNewsUseCase).ingestLatest();
    }

    private static NewsResponse sampleResponse() {
        return new NewsResponse(
                1L,
                "Title",
                "Summary",
                null,
                null,
                null,
                false,
                "https://example.com/1",
                null,
                "Reuters",
                NewsCategory.STOCK,
                Instant.parse("2026-05-23T08:00:00Z"),
                List.of("THYAO"),
                List.of("bist")
        );
    }
}
