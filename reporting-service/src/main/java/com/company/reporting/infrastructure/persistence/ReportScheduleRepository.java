package com.company.reporting.infrastructure.persistence;

import com.company.reporting.domain.ReportSchedule;
import com.company.reporting.domain.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Long> {

    Optional<ReportSchedule> findByScheduleUuid(UUID scheduleUuid);

    List<ReportSchedule> findByStatusAndNextRunAtLessThanEqual(
            ScheduleStatus status,
            LocalDateTime nextRunAt
    );

    List<ReportSchedule> findAllByOrderByCreatedAtDesc();
}