package com.company.newsservice.bootstrap.config.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code news.instrument.matched} gibi outbound event'ler için JSON {@link KafkaTemplate} producer bean'leri.
 */
@Configuration
public class KafkaProducerConfig {

    /** String key + JSON value serializer'lı Kafka producer factory. */
    @Bean
    public ProducerFactory<String, Object> newsKafkaProducerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers
    ) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /** news-service Kafka publish işlemleri için paylaşılan {@link KafkaTemplate}. */
    @Bean
    public KafkaTemplate<String, Object> newsKafkaTemplate(ProducerFactory<String, Object> newsKafkaProducerFactory) {
        return new KafkaTemplate<>(newsKafkaProducerFactory);
    }
}
