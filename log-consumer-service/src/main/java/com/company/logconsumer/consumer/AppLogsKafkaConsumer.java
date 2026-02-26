package com.company.logconsumer.consumer;

import com.company.logconsumer.model.AppLogEvent;
import com.company.logconsumer.service.OpenSearchLogIndexer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppLogsKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final OpenSearchLogIndexer indexer;

    @KafkaListener(
            topics = "${log-consumer.topic}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String rawJson, Acknowledgment ack) {
        try {
            AppLogEvent event = objectMapper.readValue(rawJson, AppLogEvent.class);
            indexer.index(event);
            ack.acknowledge();
        } catch (Exception ex) {
            // Poison message riskini yönetmek için:
            // Şimdilik loglayıp ACK vermiyoruz -> retry olur.
            // İstersen burada DLQ ekleriz (bir sonraki adım).
            log.error("Failed to process log event. raw={}", rawJson, ex);
        }
    }
}