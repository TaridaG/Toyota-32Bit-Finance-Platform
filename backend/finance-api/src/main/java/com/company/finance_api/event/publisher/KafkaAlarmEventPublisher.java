package com.company.finance_api.event.publisher;

import com.company.finance_api.event.AlarmTriggeredEvent;
import com.company.finance_api.event.kafka.KafkaTopics;
import com.company.finance_api.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("kafka")
@RequiredArgsConstructor
public class KafkaAlarmEventPublisher implements AlarmEventPublisher {

    private final OutboxService outboxService;

    @Override
    public void publish(AlarmTriggeredEvent event) {
        // key olarak userId veya instrumentSymbol kullanılabilir; partitioning için mantıklı
        String key = event.getUserId() == null ? null : event.getUserId().toString();
        outboxService.enqueue(KafkaTopics.ALARM_TRIGGERED, key, event);
    }
}