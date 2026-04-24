package com.company.finance_api.service;

import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.dto.*;

import java.time.Instant;
import java.util.List;

public interface ChartService {

    List<CandlestickResponse> getCandlesticks(
            Long instrumentId,
            Instant from,
            Instant to,
            PriceType priceType
    );

    List<TradeMarkerResponse> getMyTrades(
            Long instrumentId
    );

    List<AlarmLineResponse> getMyAlarms(
            Long instrumentId
    );
}