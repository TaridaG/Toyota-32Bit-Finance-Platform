package com.company.reporting.dto;

import com.company.reporting.domain.enums.ExportFormat;
import com.company.reporting.domain.enums.ScheduleFrequency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;

@Getter
@Setter
public class CreatePortfolioReportScheduleRequest {

    @NotNull
    private ExportFormat exportFormat;

    @NotNull
    private ScheduleFrequency frequency;

    @NotNull
    @Min(0)
    @Max(23)
    private Integer preferredHour;

    @NotNull
    @Min(0)
    @Max(59)
    private Integer preferredMinute;

    private DayOfWeek preferredDayOfWeek;

    @Min(1)
    @Max(28)
    private Integer preferredDayOfMonth;
}