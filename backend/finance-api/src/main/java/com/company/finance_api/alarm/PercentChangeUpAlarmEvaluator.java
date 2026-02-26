package com.company.finance_api.alarm;

import com.company.finance_api.domain.AlarmRule;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.AlarmCondition;
import com.company.finance_api.repository.InstrumentPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PercentChangeUpAlarmEvaluator implements AlarmEvaluator {

    private final InstrumentPriceRepository priceRepository;

    @Override
    public AlarmCondition supports() {
        return AlarmCondition.PERCENT_CHANGE_UP;
    }

    @Override
    public boolean evaluate(AlarmRule rule, InstrumentPrice currentPrice) {

        Optional<InstrumentPrice> previousOpt =
                priceRepository.findTopByInstrumentAndPriceTypeOrderByTimestampDesc(
                        currentPrice.getInstrument(),
                        currentPrice.getPriceType()
                );

        if (previousOpt.isEmpty()) return false;

        BigDecimal previous = previousOpt.get().getPrice();
        BigDecimal current = currentPrice.getPrice();

        if (previous.compareTo(BigDecimal.ZERO) == 0) return false;

        BigDecimal percent =
                current.subtract(previous)
                        .divide(previous, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        return percent.compareTo(rule.getThreshold()) >= 0;
    }
}