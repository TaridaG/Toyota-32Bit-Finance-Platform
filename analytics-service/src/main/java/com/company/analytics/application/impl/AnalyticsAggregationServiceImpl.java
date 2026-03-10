package com.company.analytics.application.impl;

import com.company.analytics.application.AnalyticsAggregationService;
import com.company.analytics.domain.AnalyticsTradeAggregateDaily;
import com.company.analytics.event.TransactionExecutedEvent;
import com.company.analytics.infrastructure.persistence.AnalyticsTradeAggregateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

                    AnalyticsTradeAggregateDaily a =
                            new AnalyticsTradeAggregateDaily();

                    a.setInstrumentId(event.getInstrumentId());
                    a.setInstrumentSymbol(event.getInstrumentSymbol());
                    a.setTradeDate(tradeDate);
                    a.setTradeCount(0L);
                    a.setTotalVolume(BigDecimal.ZERO);
                    a.setBuyVolume(BigDecimal.ZERO);
                    a.setSellVolume(BigDecimal.ZERO);

                    return a;
                });

        agg.setTradeCount(agg.getTradeCount() + 1);

        agg.setTotalVolume(
                agg.getTotalVolume().add(event.getQuantity())
        );

        if ("BUY".equals(event.getType())) {
            agg.setBuyVolume(
                    agg.getBuyVolume().add(event.getQuantity())
            );
        } else {
            agg.setSellVolume(
                    agg.getSellVolume().add(event.getQuantity())
            );
        }

        repository.save(agg);
    }
}