package com.company.finance_api.service;

import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.dto.*;
import com.company.finance_api.alarm.infrastructure.http.dto.AlarmLineResponse;
import java.time.Instant;
import java.util.List;

/** ChartService iş mantığını uygular (chart service). */
public interface ChartService {

  List<CandlestickResponse> getCandlesticks(
      Long instrumentId, Instant from, Instant to, PriceType priceType);

  List<TradeMarkerResponse> getMyTrades(Long instrumentId);

  List<AlarmLineResponse> getMyAlarms(Long instrumentId);
}
