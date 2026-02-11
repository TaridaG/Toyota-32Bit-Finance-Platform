package com.company.finance_api.service.impl;

import com.company.finance_api.alarm.AlarmEvaluator;
import com.company.finance_api.alarm.AlarmEvaluatorFactory;
import com.company.finance_api.domain.AlarmRule;
import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.AlarmCondition;
import com.company.finance_api.dto.AlarmResponse;
import com.company.finance_api.event.AlarmTriggeredEvent;
import com.company.finance_api.exception.AccessDeniedBusinessException;
import com.company.finance_api.exception.ResourceNotFoundException;
import com.company.finance_api.repository.AlarmRuleRepository;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.service.AlarmService;

import lombok.extern.slf4j.Slf4j;
import com.company.finance_api.event.publisher.AlarmEventPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
@Transactional
public class AlarmServiceImpl implements AlarmService {

    private final AlarmRuleRepository alarmRuleRepository;
    private final AlarmEvaluatorFactory evaluatorFactory;
    private final AlarmEventPublisher alarmEventPublisher;
    private final InstrumentRepository instrumentRepository;
    private final UserRepository userRepository;
    private static final Logger log =
            LoggerFactory.getLogger(AlarmServiceImpl.class);

    public AlarmServiceImpl(AlarmRuleRepository alarmRuleRepository, AlarmEvaluatorFactory evaluatorFactory, AlarmEventPublisher alarmEventPublisher,InstrumentRepository instrumentRepository, UserRepository userRepository) {
        this.alarmRuleRepository = alarmRuleRepository;
        this.evaluatorFactory = evaluatorFactory;
        this.alarmEventPublisher = alarmEventPublisher;
        this.instrumentRepository = instrumentRepository;
        this.userRepository = userRepository;

    }

    @Override
    public void deactivateAlarm(Long alarmId,UUID currentUserId) {

        AlarmRule alarm = alarmRuleRepository.findById(alarmId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Alarm not found: " + alarmId)
                );

        if (!alarm.isOwnedBy(currentUserId)) {
            throw new AccessDeniedBusinessException(
                    "You are not allowed to deactivate this alarm"
            );
        }

        alarm.deactivate();
        alarmRuleRepository.save(alarm);

        log.info("ALARM_DEACTIVATED alarmId={}, userId={}",
                alarmId, currentUserId);
    }

    @Override
    public void createAlarm(
            UUID userId,
            Long instrumentId,
            AlarmCondition condition,
            BigDecimal threshold
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Instrument instrument =     instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new IllegalStateException("Instrument not found"));

        AlarmRule alarm = new AlarmRule(
                user,
                instrument,
                condition,
                threshold
        );

        alarmRuleRepository.save(alarm);
    }
    @Override
    @Transactional(readOnly = true)
    public List<AlarmResponse> getUserAlarms(UUID userId) {

        List<AlarmRule> alarms =
                alarmRuleRepository.findByUserAndActiveTrue(
                        userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalStateException("User not found"))
                );

        return alarms.stream()
                .map(alarm -> new AlarmResponse(
                        alarm.getId(),
                        alarm.getInstrument().getSymbol(),
                        alarm.getCondition(),
                        alarm.getThreshold(),
                        alarm.isActive(),
                        alarm.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public List<AlarmRule> checkAlarms(
            Instrument instrument,
            InstrumentPrice latestPrice
    ) {

        List<AlarmRule> activeAlarms =
                alarmRuleRepository.findByInstrumentAndActiveTrue(instrument);

        List<AlarmRule> triggeredAlarms = new ArrayList<>();

        for (AlarmRule alarm : activeAlarms) {
            AlarmEvaluator evaluator =
                    evaluatorFactory.getEvaluator(alarm.getCondition());

            if (evaluator.evaluate(alarm, latestPrice)) {
                alarm.deactivate();
                alarmRuleRepository.save(alarm);
                triggeredAlarms.add(alarm);

                alarmEventPublisher.publish(
                        AlarmTriggeredEvent.of(
                                alarm.getId(),
                                alarm.getUser().getId(),
                                alarm.getInstrument().getSymbol(),
                                alarm.getCondition(),
                                latestPrice.getPrice()
                        )
                );

                log.info("ALARM_TRIGGERED user={}, instrument={}, price={}",
                        alarm.getUser().getId(),
                        alarm.getInstrument().getSymbol(),
                        latestPrice.getPrice()
                );

            }
        }

        return triggeredAlarms;
    }
    @Override
    @Transactional(readOnly = true)
    public List<AlarmRule> getActiveAlarmsForUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        return alarmRuleRepository.findByUserAndActiveTrue(user);
    }




}
