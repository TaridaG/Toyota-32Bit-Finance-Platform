package com.company.logconsumer.ingestion.infrastructure.opensearch;

import org.apache.http.StatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.opensearch.indices.PutIndexTemplateResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenSearchLogInfrastructureBootstrapTest {

    @Mock
    private OpenSearchClient openSearchClient;

    @Mock
    private RestClient restClient;

    private OpenSearchLogInfrastructureBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        bootstrap = new OpenSearchLogInfrastructureBootstrap(openSearchClient, restClient);
        ReflectionTestUtils.setField(bootstrap, "indexPrefix", "application-logs");
        ReflectionTestUtils.setField(bootstrap, "retentionDays", 30);
    }

    @Test
    void ensureInfrastructure_registersTemplateAndPolicy() throws Exception {
        OpenSearchIndicesClient indicesClient = mock(OpenSearchIndicesClient.class);
        when(openSearchClient.indices()).thenReturn(indicesClient);
        when(indicesClient.putIndexTemplate(any(org.opensearch.client.opensearch.indices.PutIndexTemplateRequest.class)))
                .thenReturn(mock(PutIndexTemplateResponse.class));

        Response response = mock(Response.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(201);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(restClient.performRequest(any(Request.class))).thenReturn(response);

        bootstrap.ensureInfrastructure();

        verify(indicesClient).putIndexTemplate(any(org.opensearch.client.opensearch.indices.PutIndexTemplateRequest.class));
        verify(restClient).performRequest(any(Request.class));
    }
}
