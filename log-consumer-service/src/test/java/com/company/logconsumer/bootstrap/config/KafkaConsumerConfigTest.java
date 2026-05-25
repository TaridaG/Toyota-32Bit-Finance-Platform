package com.company.logconsumer.bootstrap.config;

import com.company.logconsumer.shared.metrics.KafkaProcessingMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringJUnitConfig(classes = {KafkaConsumerConfig.class, KafkaConsumerConfigTest.MetricsTestConfig.class})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "log-consumer.group-id=log-consumer-service-test"
})
@EmbeddedKafka(partitions = 1, topics = "app.logs")
class KafkaConsumerConfigTest {

    @org.springframework.context.annotation.Configuration
    static class MetricsTestConfig {
        @org.springframework.context.annotation.Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @org.springframework.context.annotation.Bean
        KafkaProcessingMetrics kafkaProcessingMetrics(SimpleMeterRegistry registry) {
            return new KafkaProcessingMetrics(registry);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private DefaultKafkaConsumerFactory<String, String> consumerFactory;

    @org.springframework.beans.factory.annotation.Autowired
    private ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory;

    @org.springframework.beans.factory.annotation.Autowired
    private DefaultErrorHandler kafkaErrorHandler;

    @Test
    void consumerFactory_manualCommitAndEarliestOffset() {
        assertFalse((Boolean) consumerFactory.getConfigurationProperties()
                .get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
        assertEquals("earliest", consumerFactory.getConfigurationProperties()
                .get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
        assertEquals("log-consumer-service-test", consumerFactory.getConfigurationProperties()
                .get(ConsumerConfig.GROUP_ID_CONFIG));
    }

    @Test
    void listenerContainerFactory_manualImmediateAckAndErrorHandler() {
        assertEquals(ContainerProperties.AckMode.MANUAL_IMMEDIATE,
                kafkaListenerContainerFactory.getContainerProperties().getAckMode());
        assertNotNull(kafkaErrorHandler);
    }
}
