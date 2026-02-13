package com.company.finance_api.config;

import com.company.finance_api.kafka.event.MarketPriceUpdatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("kafka")
public class KafkaMarketDataConsumerConfig {

    @Bean
    public ConsumerFactory<String, MarketPriceUpdatedEvent> marketPriceConsumerFactory() {

        JsonDeserializer<MarketPriceUpdatedEvent> deserializer =
                new JsonDeserializer<>(MarketPriceUpdatedEvent.class);
        deserializer.addTrustedPackages("*");
        deserializer.ignoreTypeHeaders();

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "finance-api-market-price-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MarketPriceUpdatedEvent>
    marketPriceKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, MarketPriceUpdatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(marketPriceConsumerFactory());
        return factory;
    }
}
