package com.company.finance_api.alarm.domain.evaluator;

import com.company.finance_api.alarm.domain.enums.AlarmCondition;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** {@link AlarmCondition} başına uygun {@link AlarmEvaluator} örneğini sağlar. */
@Slf4j
@Component
public class AlarmEvaluatorFactory {

  private final Map<AlarmCondition, AlarmEvaluator> evaluatorMap;

  public AlarmEvaluatorFactory(List<AlarmEvaluator> evaluators) {
    Map<AlarmCondition, AlarmEvaluator> map = new EnumMap<>(AlarmCondition.class);
    for (AlarmEvaluator evaluator : evaluators) {
      map.put(evaluator.supports(), evaluator);
    }
    this.evaluatorMap = Collections.unmodifiableMap(map);
  }

  /** Koşula uygun evaluator döner; yoksa boş Optional. */
  public Optional<AlarmEvaluator> getEvaluator(AlarmCondition condition) {
    AlarmEvaluator evaluator = evaluatorMap.get(condition);
    if (evaluator == null) {
      log.warn("No AlarmEvaluator found for condition: {}", condition);
      return Optional.empty();
    }
    return Optional.of(evaluator);
  }
}
