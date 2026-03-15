package com.company.reporting.scheduler;

import com.company.reporting.application.ReportRequestPublisher;
import com.company.reporting.domain.ReportMetadata;
import com.company.reporting.domain.ReportSchedule;
import com.company.reporting.domain.enums.ReportType;
import com.company.reporting.domain.enums.ScheduleFrequency;
import com.company.reporting.domain.enums.ScheduleStatus;
import com.company.reporting.event.ReportRequestedEvent;
import com.company.reporting.infrastructure.persistence.ReportMetadataRepository;
import com.company.reporting.infrastructure.persistence.ReportScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportScheduleRunner {

    private final ReportScheduleRepository reportScheduleRepository;
    private final ReportMetadataRepository reportMetadataRepository;
    private final ReportRequestPublisher reportRequestPublisher;

    @Scheduled(fixedDelayString = "${reporting.scheduler.fixed-delay-ms:60000}")
    @Transactional
    public void runDueSchedules() {
        LocalDateTime now = LocalDateTime.now();

        List<ReportSchedule> dueSchedules =
                reportScheduleRepository.findByStatusAndNextRunAtLessThanEqual(
                        ScheduleStatus.ACTIVE,
                        now
                );

        for (ReportSchedule schedule : dueSchedules) {
            ReportMetadata metadata = ReportMetadata.create(
                    ReportType.PORTFOLIO,
                    schedule.getExportFormat(),
                    null
            );

            reportMetadataRepository.save(metadata);

            ReportRequestedEvent event = new ReportRequestedEvent(
                    metadata.getReportUuid(),
                    schedule.getReportType(),
                    schedule.getExportFormat(),
                    null,
                    null,
                    null
            );

            reportRequestPublisher.publish(event);

            LocalDateTime nextRunAt = computeNextRun(schedule, now);

            schedule.markExecuted(now, nextRunAt);
            reportScheduleRepository.save(schedule);

            log.info("Scheduled report published scheduleId={} reportId={} nextRunAt={}",
                    schedule.getScheduleUuid(), metadata.getReportUuid(), nextRunAt);
        }
    }

    private LocalDateTime computeNextRun(ReportSchedule schedule, LocalDateTime now) {
        return switch (schedule.getFrequency()) {
            case DAILY -> now.plusDays(1)
                    .withHour(schedule.getPreferredHour())
                    .withMinute(schedule.getPreferredMinute())
                    .withSecond(0)
                    .withNano(0);

            case WEEKLY -> now.plusWeeks(1)
                    .with(schedule.getPreferredDayOfWeek())
                    .withHour(schedule.getPreferredHour())
                    .withMinute(schedule.getPreferredMinute())
                    .withSecond(0)
                    .withNano(0);

            case MONTHLY -> now.plusMonths(1)
                    .withDayOfMonth(schedule.getPreferredDayOfMonth())
                    .withHour(schedule.getPreferredHour())
                    .withMinute(schedule.getPreferredMinute())
                    .withSecond(0)
                    .withNano(0);
        };
    }
}