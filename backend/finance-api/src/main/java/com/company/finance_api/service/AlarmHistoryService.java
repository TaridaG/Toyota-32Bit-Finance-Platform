package com.company.finance_api.service;

import com.company.finance_api.dto.AlarmHistoryResponse;

import java.util.List;

public interface AlarmHistoryService {
    List<AlarmHistoryResponse> getMyTimeline();
}