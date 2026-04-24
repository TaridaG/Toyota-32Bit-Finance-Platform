package com.company.analytics.infrastructure.kafka;

import com.company.analytics.event.FxSnapshotUpdatedEvent;
import com.company.analytics.event.MarketPriceUpdatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AnalyticsKafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, MarketPriceUpdatedEvent> analyticsConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        JsonDeserializer<MarketPriceUpdatedEvent> deserializer =
                new JsonDeserializer<>(MarketPriceUpdatedEvent.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        ErrorHandlingDeserializer<MarketPriceUpdatedEvent> errorHandling =
                new ErrorHandlingDeserializer<>(deserializer);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "analytics-service");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandling);
    }

    @Bean
    public ProducerFactory<String, Object> analyticsDlqProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> analyticsDlqKafkaTemplate(
            ProducerFactory<String, Object> analyticsDlqProducerFactory
    ) {
        return new KafkaTemplate<>(analyticsDlqProducerFactory);
    }

    @Bean
    public DefaultErrorHandler analyticsKafkaErrorHandler(
            KafkaTemplate<String, Object> analyticsDlqKafkaTemplate
    ) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        analyticsDlqKafkaTemplate,
                        (record, ex) -> new TopicPartition(record.topic() + ".dlq", record.partition())
                );

        FixedBackOff backOff = new FixedBackOff(2000L, 3L);
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MarketPriceUpdatedEvent>
    analyticsKafkaListenerContainerFactory(
            ConsumerFactory<String, MarketPriceUpdatedEvent> analyticsConsumerFactory,
            DefaultErrorHandler analyticsKafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, MarketPriceUpdatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(analyticsConsumerFactory);
        factory.setCommonErrorHandler(analyticsKafkaErrorHandler);
        factory.setConcurrency(3);
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }

    @Bean
    public ConsumerFactory<String, FxSnapshotUpdatedEvent> analyticsFxSnapshotConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        JsonDeserializer<FxSnapshotUpdatedEvent> deserializer =
                new JsonDeserializer<>(FxSnapshotUpdatedEvent.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        ErrorHandlingDeserializer<FxSnapshotUpdatedEvent> errorHandling =
                new ErrorHandlingDeserializer<>(deserializer);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "analytics-service-fx-snapshot");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandling);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FxSnapshotUpdatedEvent>
    analyticsFxSnapshotKafkaListenerContainerFactory(
            ConsumerFactory<String, FxSnapshotUpdatedEvent> analyticsFxSnapshotConsumerFactory,
            DefaultErrorHandler analyticsKafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, FxSnapshotUpdatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(analyticsFxSnapshotConsumerFactory);
        factory.setCommonErrorHandler(analyticsKafkaErrorHandler);
        factory.setConcurrency(1);
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}