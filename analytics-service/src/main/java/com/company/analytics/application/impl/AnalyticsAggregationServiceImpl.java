package com.company.analytics.application.impl;

import com.company.analytics.application.AnalyticsAggregationService;
import com.company.analytics.domain.AnalyticsTradeAggregateDaily;
import com.company.analytics.event.TransactionExecutedEvent;
import com.company.analytics.infrastructure.persistence.AnalyticsTradeAggregateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class AnalyticsAggregationServiceImpl implements AnalyticsAggregationService {

    private final AnalyticsTradeAggregateRepository repository;

    @Transactional
    @Override
    public void process(TransactionExecutedEvent event) {

        LocalDate tradeDate = event.getExecutedAt()
                .atZone(ZoneOffset.UTC)
                .toLocalDate();

        AnalyticsTradeAggregateDaily agg =
                repository.findByInstrumentIdAndTradeDate(
                        event.getInstrumentId(),
                        tradeDate
                ).orElseGet(() -> {

                    AnalyticsTradeAggregateDaily a = new AnalyticsTradeAggregateDaily();

                    a.setInstrumentId(event.getInstrumentId());
                    a.setInstrumentSymbol(event.getInstrumentSymbol());
                    a.setTradeDate(tradeDate);

                    a.setTradeCount(0L);
                    a.setTotalVolume(BigDecimal.ZERO);

                    a.setBuyVolume(BigDecimal.ZERO);
                    a.setSellVolume(BigDecimal.ZERO);

                    a.setAvgPrice(BigDecimal.ZERO);
                    a.setMinPrice(event.getPrice());
                    a.setMaxPrice(event.getPrice());

                    return a;

                });

        Long newTradeCount = agg.getTradeCount() + 1;

        BigDecimal newTotalVolume =
                agg.getTotalVolume().add(event.getQuantity());

        agg.setTradeCount(newTradeCount);
        agg.setTotalVolume(newTotalVolume);

        if ("BUY".equals(event.getType())) {

            agg.setBuyVolume(
                    agg.getBuyVolume().add(event.getQuantity())
            );

        } else {

            agg.setSellVolume(
                    agg.getSellVolume().add(event.getQuantity())
            );

        }

        if (event.getPrice().compareTo(agg.getMinPrice()) < 0) {
            agg.setMinPrice(event.getPrice());
        }

        if (event.getPrice().compareTo(agg.getMaxPrice()) > 0) {
            agg.setMaxPrice(event.getPrice());
        }

        BigDecimal totalTradeValue =
                agg.getAvgPrice()
                        .multiply(BigDecimal.valueOf(agg.getTradeCount() - 1))
                        .add(event.getPrice());

        BigDecimal avgPrice =
                totalTradeValue.divide(
                        BigDecimal.valueOf(newTradeCount),
                        8,
                        RoundingMode.HALF_UP
                );

        agg.setAvgPrice(avgPrice);

        repository.save(agg);
    }
}