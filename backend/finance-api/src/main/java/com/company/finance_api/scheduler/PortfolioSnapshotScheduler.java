package com.company.finance_api.scheduler;

import com.company.finance_api.service.PortfolioSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioSnapshotScheduler {

    private final PortfolioSnapshotService snapshotService;

    @Scheduled(fixedRateString = "${portfolio.snapshot.fixed-rate-ms:600000}")
    public void snapshot() {
        log.info("Creating portfolio snapshots for all active users...");
        snapshotService.createSnapshotsForAllUsers();
    }
}