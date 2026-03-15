package com.company.reporting.application.impl;

import com.company.reporting.application.ReportScheduleService;
import com.company.reporting.domain.ReportSchedule;
import com.company.reporting.domain.enums.ScheduleFrequency;
import com.company.reporting.dto.CreatePortfolioReportScheduleRequest;
import com.company.reporting.dto.ReportScheduleResponse;
import com.company.reporting.infrastructure.persistence.ReportScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportScheduleServiceImpl implements ReportScheduleService {

    private final ReportScheduleRepository repository;

    @Override
    public ReportScheduleResponse createPortfolioSchedule(CreatePortfolioReportScheduleRequest request) {
        validateRequest(request);

        LocalDateTime nextRunAt = computeInitialNextRun(request);

        ReportSchedule schedule = ReportSchedule.createPortfolioSchedule(
                request.getExportFormat(),
                request.getFrequency(),
                request.getPreferredHour(),
                request.getPreferredMinute(),
                request.getPreferredDayOfWeek(),
                request.getPreferredDayOfMonth(),
                nextRunAt
        );

        repository.save(schedule);

        return ReportScheduleResponse.from(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportScheduleResponse> listSchedules() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ReportScheduleResponse::from)
                .toList();
    }

    @Override
    public ReportScheduleResponse pauseSchedule(UUID scheduleId) {
        ReportSchedule schedule = repository.findByScheduleUuid(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));

        schedule.pause();
        repository.save(schedule);

        return ReportScheduleResponse.from(schedule);
    }

    @Override
    public ReportScheduleResponse activateSchedule(UUID scheduleId) {
        ReportSchedule schedule = repository.findByScheduleUuid(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));

        LocalDateTime nextRunAt = recomputeNextRun(schedule);

        schedule.activate(nextRunAt);
        repository.save(schedule);

        return ReportScheduleResponse.from(schedule);
    }

    private void validateRequest(CreatePortfolioReportScheduleRequest request) {
        if (request.getFrequency() == ScheduleFrequency.WEEKLY && request.getPreferredDayOfWeek() == null) {
            throw new IllegalArgumentException("preferredDayOfWeek is required for WEEKLY schedules");
        }

        if (request.getFrequency() == ScheduleFrequency.MONTHLY && request.getPreferredDayOfMonth() == null) {
            throw new IllegalArgumentException("preferredDayOfMonth is required for MONTHLY schedules");
        }
    }

    private LocalDateTime computeInitialNextRun(CreatePortfolioReportScheduleRequest request) {
        LocalDateTime now = LocalDateTime.now();

        return switch (request.getFrequency()) {
            case DAILY -> buildNextDaily(now, request.getPreferredHour(), request.getPreferredMinute());
            case WEEKLY -> buildNextWeekly(now, request.getPreferredHour(), request.getPreferredMinute(),
                    request.getPreferredDayOfWeek());
            case MONTHLY -> buildNextMonthly(now, request.getPreferredHour(), request.getPreferredMinute(),
                    request.getPreferredDayOfMonth());
        };
    }

    private LocalDateTime recomputeNextRun(ReportSchedule schedule) {
        LocalDateTime now = LocalDateTime.now();

        return switch (schedule.getFrequency()) {
            case DAILY -> buildNextDaily(now, schedule.getPreferredHour(), schedule.getPreferredMinute());
            case WEEKLY -> buildNextWeekly(now, schedule.getPreferredHour(), schedule.getPreferredMinute(),
                    schedule.getPreferredDayOfWeek());
            case MONTHLY -> buildNextMonthly(now, schedule.getPreferredHour(), schedule.getPreferredMinute(),
                    schedule.getPreferredDayOfMonth());
        };
    }

    private LocalDateTime buildNextDaily(LocalDateTime now, int hour, int minute) {
        LocalDateTime candidate = now.toLocalDate().atTime(hour, minute);
        return candidate.isAfter(now) ? candidate : candidate.plusDays(1);
    }

    private LocalDateTime buildNextWeekly(LocalDateTime now, int hour, int minute, DayOfWeek dayOfWeek) {
        LocalDateTime candidate = now.toLocalDate().with(dayOfWeek).atTime(hour, minute);

        if (!candidate.isAfter(now)) {
            candidate = candidate.plusWeeks(1);
        }

        return candidate;
    }

    private LocalDateTime buildNextMonthly(LocalDateTime now, int hour, int minute, int dayOfMonth) {
        LocalDateTime candidate = now.withDayOfMonth(dayOfMonth).withHour(hour).withMinute(minute).withSecond(0).withNano(0);

        if (!candidate.isAfter(now)) {
            candidate = now.plusMonths(1)
                    .withDayOfMonth(dayOfMonth)
                    .withHour(hour)
                    .withMinute(minute)
                    .withSecond(0)
                    .withNano(0);
        }

        return candidate;
    }
}