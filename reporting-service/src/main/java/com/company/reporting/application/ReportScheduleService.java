package com.company.reporting.application;

import com.company.reporting.dto.CreatePortfolioReportScheduleRequest;
import com.company.reporting.dto.ReportScheduleResponse;

import java.util.List;
import java.util.UUID;

public interface ReportScheduleService {

    ReportScheduleResponse createPortfolioSchedule(CreatePortfolioReportScheduleRequest request);

    List<ReportScheduleResponse> listSchedules();

    ReportScheduleResponse pauseSchedule(UUID scheduleId);

    ReportScheduleResponse activateSchedule(UUID scheduleId);
}