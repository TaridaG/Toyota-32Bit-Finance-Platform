package com.company.finance_api.alarm;

import com.company.finance_api.domain.enums.AlarmCondition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

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

    public Optional<AlarmEvaluator> getEvaluator(AlarmCondition condition) {
        AlarmEvaluator evaluator = evaluatorMap.get(condition);
        if (evaluator == null) {
            log.warn("No AlarmEvaluator found for condition: {}", condition);
            return Optional.empty();
        }
        return Optional.of(evaluator);
    }
}
