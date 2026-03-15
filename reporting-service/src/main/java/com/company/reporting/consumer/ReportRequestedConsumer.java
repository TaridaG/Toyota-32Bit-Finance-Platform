package com.company.reporting.consumer;

import com.company.reporting.application.ReportProcessingService;
import com.company.reporting.event.ReportRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportRequestedConsumer {

    private final ReportProcessingService reportProcessingService;

    @KafkaListener(
            topics = "report.requested",
            groupId = "reporting-service",
            containerFactory = "reportingKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, ReportRequestedEvent> record) {
        ReportRequestedEvent event = record.value();

        log.info("Report request consumed reportId={} symbol={} format={}",
                event.getReportId(), event.getSymbol(), event.getExportFormat());

        reportProcessingService.process(event);
    }
}