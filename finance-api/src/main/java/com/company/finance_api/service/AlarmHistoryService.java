package com.company.finance_api.service;

import com.company.finance_api.dto.AlarmHistoryResponse;
import java.util.List;

/** AlarmHistoryService iş mantığını uygular (alarm history service). */
public interface AlarmHistoryService {
  /** getMyTimeline sözleşmesi. */
  List<AlarmHistoryResponse> getMyTimeline();
}
