package com.company.marketdataservice.viop.infrastructure.scheduler;

import com.company.marketdataservice.viop.application.ViopIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "market.viop", name = "enabled", havingValue = "true")
public class ViopIngestScheduler {

    private final ViopIngestService viopIngestService;

    @Scheduled(cron = "${market.viop.cron:0 40 19 * * MON-FRI}", zone = "${market.viop.zone:Europe/Istanbul}")
    public void runDailyIngest() {
        try {
            viopIngestService.ingestDaily();
        } catch (Exception ex) {
            log.warn("VIOP_INGEST_SCHEDULER_FAILED reason={}", ex.toString(), ex);
        }
    }
}

