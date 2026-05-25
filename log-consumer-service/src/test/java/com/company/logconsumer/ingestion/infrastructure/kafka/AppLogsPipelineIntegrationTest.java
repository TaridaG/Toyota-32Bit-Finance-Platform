package com.company.logconsumer.ingestion.infrastructure.kafka;

import com.company.logconsumer.LogConsumerApplication;
import com.company.logconsumer.ingestion.domain.AppLogEvent;
import com.company.logconsumer.ingestion.infrastructure.opensearch.OpenSearchLogIndexer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.RestClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.opensearch.indices.PutIndexTemplateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * End-to-end: Kafka {@code app.logs} → {@link AppLogsKafkaConsumer} → {@link OpenSearchLogIndexer}.
 */
@SpringBootTest(classes = LogConsumerApplication.class)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "app.logs")
class AppLogsPipelineIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @MockBean
    private OpenSearchLogIndexer indexer;

    @MockBean
    private OpenSearchClient openSearchClient;

    @MockBean
    private RestClient restClient;

    private KafkaTemplate<String, String> producer;

    @BeforeEach
    void setUp() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));

        OpenSearchIndicesClient indicesClient = mock(OpenSearchIndicesClient.class);
        when(openSearchClient.indices()).thenReturn(indicesClient);
        when(indicesClient.putIndexTemplate(any(org.opensearch.client.opensearch.indices.PutIndexTemplateRequest.class)))
                .thenReturn(mock(PutIndexTemplateResponse.class));
    }

    @Test
    void kafkaToOpenSearchPipeline_indexesLog4jLayoutPayload() {
        String payload = """
                {
                  "@timestamp": "2026-05-25T12:00:00.000Z",
                  "level": "INFO",
                  "service": "finance-api",
                  "message": "pipeline-integration-ok",
                  "logger_name": "com.company.test.Pipeline",
                  "thread_name": "test-thread"
                }
                """;

        producer.send("app.logs", payload);

        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(250)).untilAsserted(() ->
                verify(indexer, atLeastOnce()).index(argThat(event ->
                        event != null
                                && "finance-api".equals(event.serviceName())
                                && "pipeline-integration-ok".equals(event.message())
                ))
        );
    }
}
