package com.company.analytics.processing.application;

import com.company.analytics.processing.application.RSIService;
import com.company.analytics.processing.domain.AnalyticsPriceCandleDaily;
import com.company.analytics.processing.domain.AnalyticsRSI;
import com.company.analytics.processing.domain.event.AnalyticsMarketPriceEvent;
import com.company.analytics.processing.infrastructure.persistence.AnalyticsPriceCandleDailyRepository;
import com.company.analytics.processing.infrastructure.persistence.AnalyticsRSIRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/** Günlük candle kapanış fiyatlarından RSI14 hesaplayıp saklayan servis. */
@Service
@RequiredArgsConstructor
public class RSIServiceImpl implements RSIService {

    private final AnalyticsPriceCandleDailyRepository candleRepository;

    private final AnalyticsRSIRepository repository;

    /** Yeterli günlük candle verisi varsa RSI14 hesaplar ve trade date bazında kaydeder. */
    @Transactional
    @Override
    public void process(AnalyticsMarketPriceEvent event) {

        LocalDate tradeDate =
                event.occurredAt()
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate();

        List<AnalyticsPriceCandleDaily> candles =
                candleRepository
                        .findByInstrumentIdOrderByCandleDateAsc(
                                event.instrumentId()
                        );

        if(candles.size() < 15) return;

        BigDecimal gain = BigDecimal.ZERO;
        BigDecimal loss = BigDecimal.ZERO;

        for(int i = candles.size()-14; i < candles.size(); i++){

            BigDecimal diff =
                    candles.get(i).getClosePrice()
                            .subtract(
                                    candles.get(i-1).getClosePrice()
                            );

            if(diff.compareTo(BigDecimal.ZERO) > 0){

                gain = gain.add(diff);

            }else{

                loss = loss.add(diff.abs());

            }

        }

        BigDecimal avgGain =
                gain.divide(BigDecimal.valueOf(14),8,RoundingMode.HALF_UP);

        BigDecimal avgLoss =
                loss.divide(BigDecimal.valueOf(14),8,RoundingMode.HALF_UP);

        if(avgLoss.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal rs =
                avgGain.divide(avgLoss,8,RoundingMode.HALF_UP);

        BigDecimal rsi =
                BigDecimal.valueOf(100)
                        .subtract(
                                BigDecimal.valueOf(100)
                                        .divide(
                                                BigDecimal.ONE.add(rs),
                                                8,
                                                RoundingMode.HALF_UP
                                        )
                        );

        AnalyticsRSI entity =
                repository.findByInstrumentIdAndTradeDate(
                        event.instrumentId(),
                        tradeDate
                ).orElseGet(AnalyticsRSI::new);

        entity.setInstrumentId(event.instrumentId());
        entity.setInstrumentSymbol(event.instrumentSymbol());
        entity.setTradeDate(tradeDate);
        entity.setRsi14(rsi);

        repository.save(entity);

    }
}