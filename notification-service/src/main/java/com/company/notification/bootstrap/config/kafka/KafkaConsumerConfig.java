package com.company.notification.bootstrap.config.kafka;

import com.company.notification.alarm.infrastructure.kafka.messaging.AlarmTriggeredMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;
import com.company.notification.report.infrastructure.kafka.messaging.ReportCompletedMessage;
import com.company.notification.report.infrastructure.kafka.messaging.ReportFailedMessage;
import com.company.notification.insight.infrastructure.kafka.messaging.AnalyticsInsightMessage;
import com.company.notification.watchlist.infrastructure.kafka.messaging.WatchlistItemAddedMessage;
import com.company.notification.insight.infrastructure.kafka.messaging.NewsInstrumentMatchedMessage;
import com.company.notification.watchlist.infrastructure.kafka.messaging.WatchlistItemRemovedMessage;
import com.company.notification.security.infrastructure.kafka.messaging.LoginSecurityAlertMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Tüm inbound topic'ler için Kafka listener factory'leri, JSON deserializer'lar ve dead-letter publishing.
 */
@Configuration
public class KafkaConsumerConfig {
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public ConsumerFactory<String, AlarmTriggeredMessage> consumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        JsonDeserializer<AlarmTriggeredMessage> deserializer =
                new JsonDeserializer<>(AlarmTriggeredMessage.class);

        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ProducerFactory<String, Object> dlqProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> dlqKafkaTemplate(
            ProducerFactory<String, Object> dlqProducerFactory
    ) {
        return new KafkaTemplate<>(dlqProducerFactory);
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> dlqKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        dlqKafkaTemplate,
                        (record, ex) -> new TopicPartition(record.topic() + ".dlq", record.partition())
                );

        FixedBackOff backOff = new FixedBackOff(2000L, 3L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("Retry attempt {} for topic={} key={} exception={}",
                        deliveryAttempt, record.topic(), record.key(), ex.getMessage())
        );
        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AlarmTriggeredMessage> kafkaListenerContainerFactory(
            ConsumerFactory<String, AlarmTriggeredMessage> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, AlarmTriggeredMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, LoginSecurityAlertMessage> loginSecurityConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        JsonDeserializer<LoginSecurityAlertMessage> deserializer =
                new JsonDeserializer<>(LoginSecurityAlertMessage.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LoginSecurityAlertMessage> loginSecurityKafkaListenerContainerFactory(
            ConsumerFactory<String, LoginSecurityAlertMessage> loginSecurityConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, LoginSecurityAlertMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(loginSecurityConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ReportCompletedMessage> reportCompletedConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        JsonDeserializer<ReportCompletedMessage> deserializer =
                new JsonDeserializer<>(ReportCompletedMessage.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-report-completed");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReportCompletedMessage>
    reportCompletedKafkaListenerContainerFactory(
            ConsumerFactory<String, ReportCompletedMessage> reportCompletedConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, ReportCompletedMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(reportCompletedConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ReportFailedMessage> reportFailedConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        JsonDeserializer<ReportFailedMessage> deserializer =
                new JsonDeserializer<>(ReportFailedMessage.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-report-failed");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReportFailedMessage>
    reportFailedKafkaListenerContainerFactory(
            ConsumerFactory<String, ReportFailedMessage> reportFailedConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, ReportFailedMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(reportFailedConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, WatchlistItemAddedMessage> watchlistItemAddedConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        JsonDeserializer<WatchlistItemAddedMessage> deserializer =
                new JsonDeserializer<>(WatchlistItemAddedMessage.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-watchlist-projection");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WatchlistItemAddedMessage>
    watchlistItemAddedKafkaListenerContainerFactory(
            ConsumerFactory<String, WatchlistItemAddedMessage> watchlistItemAddedConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, WatchlistItemAddedMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(watchlistItemAddedConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, WatchlistItemRemovedMessage> watchlistItemRemovedConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        JsonDeserializer<WatchlistItemRemovedMessage> deserializer =
                new JsonDeserializer<>(WatchlistItemRemovedMessage.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-watchlist-projection");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WatchlistItemRemovedMessage>
    watchlistItemRemovedKafkaListenerContainerFactory(
            ConsumerFactory<String, WatchlistItemRemovedMessage> watchlistItemRemovedConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, WatchlistItemRemovedMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(watchlistItemRemovedConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, AnalyticsInsightMessage> analyticsInsightConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        JsonDeserializer<AnalyticsInsightMessage> deserializer =
                new JsonDeserializer<>(AnalyticsInsightMessage.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-insight-simple");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AnalyticsInsightMessage>
    analyticsInsightKafkaListenerContainerFactory(
            ConsumerFactory<String, AnalyticsInsightMessage> analyticsInsightConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, AnalyticsInsightMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(analyticsInsightConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, NewsInstrumentMatchedMessage> newsInstrumentMatchedConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        JsonDeserializer<NewsInstrumentMatchedMessage> deserializer =
                new JsonDeserializer<>(NewsInstrumentMatchedMessage.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-news-matched");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NewsInstrumentMatchedMessage>
    newsInstrumentMatchedKafkaListenerContainerFactory(
            ConsumerFactory<String, NewsInstrumentMatchedMessage> newsInstrumentMatchedConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, NewsInstrumentMatchedMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(newsInstrumentMatchedConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }
}
