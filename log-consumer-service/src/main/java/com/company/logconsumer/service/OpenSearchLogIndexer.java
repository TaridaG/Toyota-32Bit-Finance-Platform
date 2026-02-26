package com.company.logconsumer.service;

import com.company.logconsumer.model.AppLogEvent;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenSearchLogIndexer {

    private final OpenSearchClient client;
    private final IndexNameResolver indexNameResolver;

    @Value("${opensearch.index-prefix:application-logs}")
    private String indexPrefix;

    public void index(AppLogEvent event) throws Exception {
        String index = indexNameResolver.todayIndex(indexPrefix);

        // id vermiyoruz (OpenSearch auto id); istersen correlationId+timestamp ile deterministic yaparız
        IndexRequest<AppLogEvent> req = IndexRequest.of(i -> i
                .index(index)
                .document(event)
        );
        client.index(req);
    }
}