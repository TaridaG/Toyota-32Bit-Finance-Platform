package com.company.finance_api.alarm;

import com.company.finance_api.domain.AlarmRule;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.AlarmCondition;
import com.company.finance_api.repository.InstrumentPriceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Önceki fiyata göre yüzde düşüş eşiğini aşan alarm evaluator. */
@Component
@RequiredArgsConstructor
public class PercentChangeDownAlarmEvaluator implements AlarmEvaluator {

  private final InstrumentPriceRepository priceRepository;

  @Override
  public AlarmCondition supports() {
    return AlarmCondition.PERCENT_CHANGE_DOWN;
  }

  @Override
  public boolean evaluate(AlarmRule rule, InstrumentPrice currentPrice) {

    Optional<InstrumentPrice> previousOpt =
        priceRepository.findFirstByInstrumentAndPriceTypeAndTimestampLessThanOrderByTimestampDesc(
            currentPrice.getInstrument(), currentPrice.getPriceType(), currentPrice.getTimestamp());

    if (previousOpt.isEmpty()) return false;

    BigDecimal previous = previousOpt.get().getPrice();
    BigDecimal current = currentPrice.getPrice();

    if (previous.compareTo(BigDecimal.ZERO) == 0) return false;

    BigDecimal percent =
        previous
            .subtract(current)
            .divide(previous, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

    return percent.compareTo(rule.getThreshold()) >= 0;
  }
}
