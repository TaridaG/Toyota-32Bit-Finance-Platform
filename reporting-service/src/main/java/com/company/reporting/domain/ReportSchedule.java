package com.company.reporting.domain;

import com.company.reporting.domain.enums.ExportFormat;
import com.company.reporting.domain.enums.ReportType;
import com.company.reporting.domain.enums.ScheduleFrequency;
import com.company.reporting.domain.enums.ScheduleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "report_schedule")
@Getter
@Setter
public class ReportSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_uuid", nullable = false, unique = true)
    private UUID scheduleUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 32)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_format", nullable = false, length = 16)
    private ExportFormat exportFormat;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 16)
    private ScheduleFrequency frequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ScheduleStatus status;

    @Column(name = "preferred_hour", nullable = false)
    private Integer preferredHour;

    @Column(name = "preferred_minute", nullable = false)
    private Integer preferredMinute;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_day_of_week", length = 16)
    private DayOfWeek preferredDayOfWeek;

    @Column(name = "preferred_day_of_month")
    private Integer preferredDayOfMonth;

    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static ReportSchedule createPortfolioSchedule(
            ExportFormat exportFormat,
            ScheduleFrequency frequency,
            Integer preferredHour,
            Integer preferredMinute,
            DayOfWeek preferredDayOfWeek,
            Integer preferredDayOfMonth,
            LocalDateTime nextRunAt
    ) {
        ReportSchedule schedule = new ReportSchedule();
        Instant now = Instant.now();

        schedule.setScheduleUuid(UUID.randomUUID());
        schedule.setReportType(ReportType.PORTFOLIO);
        schedule.setExportFormat(exportFormat);
        schedule.setFrequency(frequency);
        schedule.setStatus(ScheduleStatus.ACTIVE);
        schedule.setPreferredHour(preferredHour);
        schedule.setPreferredMinute(preferredMinute);
        schedule.setPreferredDayOfWeek(preferredDayOfWeek);
        schedule.setPreferredDayOfMonth(preferredDayOfMonth);
        schedule.setNextRunAt(nextRunAt);
        schedule.setCreatedAt(now);
        schedule.setUpdatedAt(now);

        return schedule;
    }

    public void markExecuted(LocalDateTime executedAt, LocalDateTime newNextRunAt) {
        this.lastRunAt = executedAt;
        this.nextRunAt = newNextRunAt;
        this.updatedAt = Instant.now();
    }

    public void pause() {
        this.status = ScheduleStatus.PAUSED;
        this.updatedAt = Instant.now();
    }

    public void activate(LocalDateTime newNextRunAt) {
        this.status = ScheduleStatus.ACTIVE;
        this.nextRunAt = newNextRunAt;
        this.updatedAt = Instant.now();
    }
}