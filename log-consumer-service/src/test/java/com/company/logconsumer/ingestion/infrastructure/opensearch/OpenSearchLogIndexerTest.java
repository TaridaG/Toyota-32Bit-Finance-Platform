package com.company.logconsumer.ingestion.infrastructure.opensearch;

import com.company.logconsumer.ingestion.domain.AppLogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenSearchLogIndexerTest {

    @Mock
    private OpenSearchClient client;

    private final IndexNameResolver indexNameResolver = new IndexNameResolver();
    private OpenSearchLogIndexer indexer;

    @BeforeEach
    void setUp() {
        indexer = new OpenSearchLogIndexer(client, indexNameResolver);
        ReflectionTestUtils.setField(indexer, "indexPrefix", "application-logs");
    }

    @Test
    void index_writesToDailyIndex() throws Exception {
        when(client.index(any(IndexRequest.class))).thenReturn(mock(org.opensearch.client.opensearch.core.IndexResponse.class));

        AppLogEvent event = new AppLogEvent(
                "2026-05-23T10:00:00Z", "INFO", "finance-api", "ok",
                null, null, null, null, null, null
        );

        indexer.index(event);

        ArgumentCaptor<IndexRequest<AppLogEvent>> captor = ArgumentCaptor.forClass(IndexRequest.class);
        verify(client).index(captor.capture());
        String expectedIndex = indexNameResolver.todayIndex("application-logs");
        assertEquals(expectedIndex, captor.getValue().index());
    }

    @Test
    void index_propagatesClientFailure() throws Exception {
        doThrow(new RuntimeException("cluster unavailable")).when(client).index(any(IndexRequest.class));

        AppLogEvent event = new AppLogEvent(
                "2026-05-23T10:00:00Z", "ERROR", "finance-api", "fail",
                null, null, null, null, null, null
        );

        assertThrows(RuntimeException.class, () -> indexer.index(event));
    }
}
