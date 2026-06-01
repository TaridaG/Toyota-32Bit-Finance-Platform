package com.company.finance_api.bootstrap.config;

import com.company.finance_api.notification.infrastructure.kafka.messaging.PortalInboxDeliverMessage;
import com.company.finance_api.shared.messaging.kafka.event.FundSnapshotUpdatedEvent;
import com.company.finance_api.shared.messaging.kafka.event.FxSnapshotUpdatedEvent;
import com.company.finance_api.shared.messaging.kafka.event.MarketPriceUpdatedEvent;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

/**
 * {@code kafka} profilinde market-data snapshot consumer factory'leri, DLQ template ve retry error
 * handler.
 */
@Slf4j
@Configuration
@Profile("kafka")
public class KafkaMarketDataConsumerConfig {

  /** Market price updated event'leri için consumer factory bean'i. */
  @Bean
  public ConsumerFactory<String, MarketPriceUpdatedEvent> marketPriceConsumerFactory() {

    JsonDeserializer<MarketPriceUpdatedEvent> deserializer =
        new JsonDeserializer<>(MarketPriceUpdatedEvent.class);
    deserializer.addTrustedPackages("*");
    deserializer.ignoreTypeHeaders();
    ErrorHandlingDeserializer<MarketPriceUpdatedEvent> errorHandling =
        new ErrorHandlingDeserializer<>(deserializer);

    Map<String, Object> props = new HashMap<>();
    props.put(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"));
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "finance-api-market-price-consumer");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

    return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandling);
  }

  /** Market price listener container factory; ortak error handler ile DLQ retry kullanır. */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, MarketPriceUpdatedEvent>
      marketPriceKafkaListenerContainerFactory(DefaultErrorHandler errorHandler) {

    ConcurrentKafkaListenerContainerFactory<String, MarketPriceUpdatedEvent> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(marketPriceConsumerFactory());
    factory.setCommonErrorHandler(errorHandler);
    return factory;
  }

  /** FX snapshot updated event'leri için consumer factory bean'i. */
  @Bean
  public ConsumerFactory<String, FxSnapshotUpdatedEvent> fxSnapshotConsumerFactory() {
    JsonDeserializer<FxSnapshotUpdatedEvent> deserializer =
        new JsonDeserializer<>(FxSnapshotUpdatedEvent.class);
    deserializer.addTrustedPackages("*");
    deserializer.ignoreTypeHeaders();
    ErrorHandlingDeserializer<FxSnapshotUpdatedEvent> errorHandling =
        new ErrorHandlingDeserializer<>(deserializer);
    Map<String, Object> props = new HashMap<>();
    props.put(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"));
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "finance-api-fx-snapshot-consumer");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandling);
  }

  /** FX snapshot listener container factory. */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, FxSnapshotUpdatedEvent>
      fxSnapshotKafkaListenerContainerFactory(DefaultErrorHandler errorHandler) {
    ConcurrentKafkaListenerContainerFactory<String, FxSnapshotUpdatedEvent> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(fxSnapshotConsumerFactory());
    factory.setCommonErrorHandler(errorHandler);
    return factory;
  }

  /** Fund snapshot updated event'leri için consumer factory bean'i. */
  @Bean
  public ConsumerFactory<String, FundSnapshotUpdatedEvent> fundSnapshotConsumerFactory() {
    JsonDeserializer<FundSnapshotUpdatedEvent> deserializer =
        new JsonDeserializer<>(FundSnapshotUpdatedEvent.class);
    deserializer.addTrustedPackages("*");
    deserializer.ignoreTypeHeaders();
    ErrorHandlingDeserializer<FundSnapshotUpdatedEvent> errorHandling =
        new ErrorHandlingDeserializer<>(deserializer);
    Map<String, Object> props = new HashMap<>();
    props.put(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"));
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "finance-api-fund-snapshot-consumer");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandling);
  }

  /** Fund snapshot listener container factory. */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, FundSnapshotUpdatedEvent>
      fundSnapshotKafkaListenerContainerFactory(DefaultErrorHandler errorHandler) {
    ConcurrentKafkaListenerContainerFactory<String, FundSnapshotUpdatedEvent> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(fundSnapshotConsumerFactory());
    factory.setCommonErrorHandler(errorHandler);
    return factory;
  }

  /** Portal inbox deliver mesajları için consumer factory. */
  @Bean
  public ConsumerFactory<String, PortalInboxDeliverMessage> portalInboxConsumerFactory() {
    JsonDeserializer<PortalInboxDeliverMessage> deserializer =
        new JsonDeserializer<>(PortalInboxDeliverMessage.class);
    deserializer.addTrustedPackages("*");
    deserializer.ignoreTypeHeaders();
    ErrorHandlingDeserializer<PortalInboxDeliverMessage> errorHandling =
        new ErrorHandlingDeserializer<>(deserializer);
    Map<String, Object> props = new HashMap<>();
    props.put(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"));
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "finance-api-portal-inbox");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandling);
  }

  /** Portal inbox listener container factory. */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, PortalInboxDeliverMessage>
      portalInboxKafkaListenerContainerFactory(DefaultErrorHandler errorHandler) {
    ConcurrentKafkaListenerContainerFactory<String, PortalInboxDeliverMessage> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(portalInboxConsumerFactory());
    factory.setCommonErrorHandler(errorHandler);
    return factory;
  }

  /** Dead-letter queue publish için minimal Kafka producer factory. */
  @Bean
  public ProducerFactory<String, Object> dlqProducerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
        System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"));
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(props);
  }

  /** DLQ topic'lerine yazmak için {@link KafkaTemplate} bean'i. */
  @Bean
  public KafkaTemplate<String, Object> dlqKafkaTemplate() {
    return new KafkaTemplate<>(dlqProducerFactory());
  }

  /** Fixed backoff + DLQ recoverer ile consumer retry error handler bean'i. */
  @Bean
  public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> dlqKafkaTemplate) {
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            dlqKafkaTemplate,
            (record, ex) -> new TopicPartition(record.topic() + ".dlq", record.partition()));

    FixedBackOff backOff = new FixedBackOff(2000L, 3);

    DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

    handler.setRetryListeners(
        (record, ex, deliveryAttempt) ->
            log.warn(
                "Retry {} for record {} due to {}",
                deliveryAttempt,
                record.value(),
                ex.getMessage()));

    return handler;
  }
}
