package com.company.analytics.application.impl;

import com.company.analytics.application.MovingAverageService;
import com.company.analytics.domain.AnalyticsMovingAverage;
import com.company.analytics.domain.AnalyticsPriceCandleDaily;
import com.company.analytics.event.TransactionExecutedEvent;
import com.company.analytics.infrastructure.persistence.AnalyticsMovingAverageRepository;
import com.company.analytics.infrastructure.persistence.AnalyticsPriceCandleDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovingAverageServiceImpl implements MovingAverageService {

    private final AnalyticsPriceCandleDailyRepository candleRepository;

    private final AnalyticsMovingAverageRepository repository;

    @Transactional
    @Override
    public void process(TransactionExecutedEvent event) {

        LocalDate tradeDate =
                event.getExecutedAt()
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate();

        List<AnalyticsPriceCandleDaily> candles =
                candleRepository
                        .findByInstrumentSymbolOrderByCandleDateAsc(
                                event.getInstrumentSymbol()
                        );

        BigDecimal ma7 = calculateMA(candles,7);
        BigDecimal ma30 = calculateMA(candles,30);
        BigDecimal ma90 = calculateMA(candles,90);

        AnalyticsMovingAverage ma =
                repository.findByInstrumentIdAndTradeDate(
                        event.getInstrumentId(),
                        tradeDate
                ).orElseGet(AnalyticsMovingAverage::new);

        ma.setInstrumentId(event.getInstrumentId());
        ma.setInstrumentSymbol(event.getInstrumentSymbol());
        ma.setTradeDate(tradeDate);

        ma.setMa7(ma7);
        ma.setMa30(ma30);
        ma.setMa90(ma90);

        repository.save(ma);

    }

    private BigDecimal calculateMA(
            List<AnalyticsPriceCandleDaily> candles,
            int period
    ){

        if(candles.size() < period) return null;

        BigDecimal sum = BigDecimal.ZERO;

        for(int i = candles.size()-period; i < candles.size(); i++){

            sum = sum.add(
                    candles.get(i).getClosePrice()
            );

        }

        return sum.divide(
                BigDecimal.valueOf(period),
                8,
                RoundingMode.HALF_UP
        );

    }

}