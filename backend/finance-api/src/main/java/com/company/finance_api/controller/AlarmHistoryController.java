package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.AlarmHistoryResponse;
import com.company.finance_api.service.AlarmHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history/alarms")
@RequiredArgsConstructor
public class AlarmHistoryController {

    private final AlarmHistoryService alarmHistoryService;

    @GetMapping
    public ApiResponse<List<AlarmHistoryResponse>> myAlarmHistory() {
        return ApiResponse.success(
                alarmHistoryService.getMyTimeline()
        );
    }
}