package com.company.finance_api.alarm.application;

import com.company.finance_api.alarm.infrastructure.http.dto.AlarmHistoryResponse;
import java.util.List;

/** AlarmHistoryService iş mantığını uygular (alarm history service). */
public interface AlarmHistoryService {
  /** getMyTimeline sözleşmesi. */
  List<AlarmHistoryResponse> getMyTimeline();
}
