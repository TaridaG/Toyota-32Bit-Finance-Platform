package com.company.finance_api.config.kafka;

import com.company.finance_api.event.AlarmTriggeredEvent;
import com.company.finance_api.event.TransactionExecutedEvent;
import com.company.finance_api.event.kafka.KafkaTopics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Component;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("kafka")
public class KafkaProducerConfig {

    private Map<String, Object> baseProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
        );
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(
                ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
                java.util.Collections.singletonList(
                        CorrelationIdProducerInterceptor.class.getName()
                )
        );
        return props;
    }

    // 🔔 ALARM
    @Bean
    public ProducerFactory<String, AlarmTriggeredEvent> alarmProducerFactory() {
        return new DefaultKafkaProducerFactory<>(baseProps());
    }

    @Bean
    public KafkaTemplate<String, AlarmTriggeredEvent> alarmKafkaTemplate() {
        return new KafkaTemplate<>(alarmProducerFactory());
    }

    // 💸 TRANSACTION
    @Bean
    public ProducerFactory<String, TransactionExecutedEvent> transactionProducerFactory() {
        return new DefaultKafkaProducerFactory<>(baseProps());
    }

    @Bean
    public KafkaTemplate<String, TransactionExecutedEvent> transactionKafkaTemplate() {
        return new KafkaTemplate<>(transactionProducerFactory());
    }
}


