package com.company.finance_api.bootstrap.config.kafka;

import com.company.finance_api.shared.messaging.event.AlarmTriggeredEvent;
import com.company.finance_api.shared.messaging.event.TransactionExecutedEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

/** {@code kafka} profilinde alarm ve transaction event'leri için Kafka producer bean'leri. */
@Configuration
@Profile("kafka")
public class KafkaProducerConfig {

  private Map<String, Object> baseProps() {
    Map<String, Object> props = new HashMap<>();
    props.put(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
        System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"));
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    props.put(
        ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
        java.util.Collections.singletonList(CorrelationIdProducerInterceptor.class.getName()));
    return props;
  }

  /** Alarm triggered event'leri için producer factory bean'i. */
  @Bean
  public ProducerFactory<String, AlarmTriggeredEvent> alarmProducerFactory() {
    return new DefaultKafkaProducerFactory<>(baseProps());
  }

  /** Alarm triggered event'leri için {@link KafkaTemplate} bean'i. */
  @Bean
  public KafkaTemplate<String, AlarmTriggeredEvent> alarmKafkaTemplate() {
    return new KafkaTemplate<>(alarmProducerFactory());
  }

  /** Transaction executed event'leri için producer factory bean'i. */
  @Bean
  public ProducerFactory<String, TransactionExecutedEvent> transactionProducerFactory() {
    return new DefaultKafkaProducerFactory<>(baseProps());
  }

  /** Transaction executed event'leri için {@link KafkaTemplate} bean'i. */
  @Bean
  public KafkaTemplate<String, TransactionExecutedEvent> transactionKafkaTemplate() {
    return new KafkaTemplate<>(transactionProducerFactory());
  }
}
