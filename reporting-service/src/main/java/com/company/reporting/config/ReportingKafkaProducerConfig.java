package com.company.reporting.config;

import com.company.reporting.event.ReportCompletedEvent;
import com.company.reporting.event.ReportFailedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ReportingKafkaProducerConfig {

    private Map<String, Object> baseProps(String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return props;
    }

    @Bean
    public ProducerFactory<String, ReportCompletedEvent> reportCompletedProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        return new DefaultKafkaProducerFactory<>(baseProps(bootstrapServers));
    }

    @Bean
    public KafkaTemplate<String, ReportCompletedEvent> reportCompletedKafkaTemplate(
            ProducerFactory<String, ReportCompletedEvent> reportCompletedProducerFactory
    ) {
        return new KafkaTemplate<>(reportCompletedProducerFactory);
    }

    @Bean
    public ProducerFactory<String, ReportFailedEvent> reportFailedProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        return new DefaultKafkaProducerFactory<>(baseProps(bootstrapServers));
    }

    @Bean
    public KafkaTemplate<String, ReportFailedEvent> reportFailedKafkaTemplate(
            ProducerFactory<String, ReportFailedEvent> reportFailedProducerFactory
    ) {
        return new KafkaTemplate<>(reportFailedProducerFactory);
    }
}