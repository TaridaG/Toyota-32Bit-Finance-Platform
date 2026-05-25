package com.company.finance_api.alarm.infrastructure.http;

import com.company.finance_api.alarm.infrastructure.http.dto.AlarmHistoryResponse;
import com.company.finance_api.alarm.application.AlarmHistoryService;
import com.company.finance_api.shared.web.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Tetiklenmiş alarm geçmişi zaman çizelgesi endpoint'i. */
@RestController
@RequestMapping("/api/history/alarms")
@RequiredArgsConstructor
public class AlarmHistoryController {

  private final AlarmHistoryService alarmHistoryService;

  /** Kullanıcının alarm tetikleme geçmişini döner. */
  @GetMapping
  public ApiResponse<List<AlarmHistoryResponse>> myAlarmHistory() {
    return ApiResponse.success(alarmHistoryService.getMyTimeline());
  }
}
