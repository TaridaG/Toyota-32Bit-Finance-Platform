package com.company.finance_api.alarm;

import com.company.finance_api.domain.enums.AlarmCondition;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class AlarmEvaluatorFactory {

    private final Map<AlarmCondition, AlarmEvaluator> evaluatorMap;

    public AlarmEvaluatorFactory(List<AlarmEvaluator> evaluators) {
        this.evaluatorMap = new EnumMap<>(AlarmCondition.class);
        evaluators.forEach(e ->
                evaluatorMap.put(e.supports(), e)
        );
    }

    public AlarmEvaluator getEvaluator(AlarmCondition condition) {
        AlarmEvaluator evaluator = evaluatorMap.get(condition);
        if (evaluator == null) {
            throw new IllegalStateException("No evaluator for condition: " + condition);
        }
        return evaluator;
    }
}
