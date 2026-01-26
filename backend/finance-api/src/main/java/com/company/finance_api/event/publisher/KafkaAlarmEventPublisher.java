package com.company.finance_api.event.publisher;

import com.company.finance_api.event.AlarmTriggeredEvent;
import com.company.finance_api.event.kafka.KafkaTopics;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;



@Component
@Profile("kafka")
public class KafkaAlarmEventPublisher implements AlarmEventPublisher {

    private final KafkaTemplate<String, AlarmTriggeredEvent> kafkaTemplate;

    public KafkaAlarmEventPublisher(
            KafkaTemplate<String, AlarmTriggeredEvent> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(AlarmTriggeredEvent event) {
        kafkaTemplate.send(
                KafkaTopics.ALARM_TRIGGERED,
                event.getUserId().toString(), // partition key
                event
        );
    }
}
