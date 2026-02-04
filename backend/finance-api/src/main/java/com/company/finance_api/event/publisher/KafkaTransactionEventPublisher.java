package com.company.finance_api.event.publisher;

import com.company.finance_api.event.TransactionExecutedEvent;
import com.company.finance_api.event.kafka.KafkaTopics;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("kafka")
public class KafkaTransactionEventPublisher implements TransactionEventPublisher {

    private final KafkaTemplate<String, TransactionExecutedEvent> kafkaTemplate;

    public KafkaTransactionEventPublisher(
            KafkaTemplate<String, TransactionExecutedEvent> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(TransactionExecutedEvent event) {
        kafkaTemplate.send(
                KafkaTopics.TRANSACTION_EXECUTED,
                event.getUserId().toString(),
                event
        );
    }
}