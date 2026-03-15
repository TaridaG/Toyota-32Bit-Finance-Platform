package com.company.reporting.dto;

import com.company.reporting.domain.ReportSchedule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReportScheduleResponse {

    private UUID scheduleId;
    private String reportType;
    private String exportFormat;
    private String frequency;
    private String status;
    private Integer preferredHour;
    private Integer preferredMinute;
    private String preferredDayOfWeek;
    private Integer preferredDayOfMonth;
    private LocalDateTime nextRunAt;
    private LocalDateTime lastRunAt;

    public static ReportScheduleResponse from(ReportSchedule schedule) {
        return ReportScheduleResponse.builder()
                .scheduleId(schedule.getScheduleUuid())
                .reportType(schedule.getReportType().name())
                .exportFormat(schedule.getExportFormat().name())
                .frequency(schedule.getFrequency().name())
                .status(schedule.getStatus().name())
                .preferredHour(schedule.getPreferredHour())
                .preferredMinute(schedule.getPreferredMinute())
                .preferredDayOfWeek(
                        schedule.getPreferredDayOfWeek() == null
                                ? null
                                : schedule.getPreferredDayOfWeek().name()
                )
                .preferredDayOfMonth(schedule.getPreferredDayOfMonth())
                .nextRunAt(schedule.getNextRunAt())
                .lastRunAt(schedule.getLastRunAt())
                .build();
    }
}