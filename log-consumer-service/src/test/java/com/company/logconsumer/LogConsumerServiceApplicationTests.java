package com.company.logconsumer;

import com.company.logconsumer.ingestion.infrastructure.opensearch.OpenSearchLogIndexer;
import org.junit.jupiter.api.Test;
import org.opensearch.client.RestClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LogConsumerServiceApplicationTests {

    @MockBean
    private OpenSearchLogIndexer indexer;

    @MockBean
    private OpenSearchClient openSearchClient;

    @MockBean
    private RestClient restClient;

    @Test
    void contextLoads() {
    }

}
