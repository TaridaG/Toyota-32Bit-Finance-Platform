package com.company.logconsumer.ingestion.infrastructure.opensearch;

import com.company.logconsumer.ingestion.domain.AppLogEvent;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Parse edilmiş {@link AppLogEvent} kayıtlarını günlük OpenSearch index'ine yazar.
 */
@Service
@RequiredArgsConstructor
public class OpenSearchLogIndexer {

    private final OpenSearchClient client;
    private final IndexNameResolver indexNameResolver;

    @Value("${opensearch.index-prefix:application-logs}")
    private String indexPrefix;

    /**
     * Olayı bugünün index'ine indexler (document id OpenSearch tarafından atanır).
     *
     * @throws Exception OpenSearch istemci veya transport hatası
     */
    public void index(AppLogEvent event) throws Exception {
        String index = indexNameResolver.todayIndex(indexPrefix);

        IndexRequest<AppLogEvent> req = IndexRequest.of(i -> i
                .index(index)
                .document(event)
        );
        client.index(req);
    }
}
