package com.company.finance_api.alarm.application;

import com.company.finance_api.alarm.domain.evaluator.AlarmEvaluator;
import com.company.finance_api.alarm.domain.evaluator.AlarmEvaluatorFactory;
import com.company.finance_api.alarm.domain.AlarmHistory;
import com.company.finance_api.alarm.domain.AlarmRule;
import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.AlarmCondition;
import com.company.finance_api.alarm.infrastructure.http.dto.AlarmResponse;
import com.company.finance_api.event.AlarmTriggeredEvent;
import com.company.finance_api.event.publisher.AlarmEventPublisher;
import com.company.finance_api.alarm.infrastructure.persistence.AlarmHistoryRepository;
import com.company.finance_api.alarm.infrastructure.persistence.AlarmRuleRepository;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.alarm.application.AlarmService;
import com.company.finance_api.shared.web.AccessDeniedBusinessException;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** AlarmServiceImpl iş mantığını uygular (alarm service). */
@Service
@Transactional
public class AlarmServiceImpl implements AlarmService {

  private final AlarmRuleRepository alarmRuleRepository;
  private final AlarmHistoryRepository alarmHistoryRepository;
  private final AlarmEvaluatorFactory evaluatorFactory;
  private final AlarmEventPublisher alarmEventPublisher;
  private final InstrumentRepository instrumentRepository;
  private final UserRepository userRepository;
  private static final Logger log = LoggerFactory.getLogger(AlarmServiceImpl.class);

  public AlarmServiceImpl(
      AlarmRuleRepository alarmRuleRepository,
      AlarmHistoryRepository alarmHistoryRepository,
      AlarmEvaluatorFactory evaluatorFactory,
      AlarmEventPublisher alarmEventPublisher,
      InstrumentRepository instrumentRepository,
      UserRepository userRepository) {
    this.alarmRuleRepository = alarmRuleRepository;
    this.alarmHistoryRepository = alarmHistoryRepository;
    this.evaluatorFactory = evaluatorFactory;
    this.alarmEventPublisher = alarmEventPublisher;
    this.instrumentRepository = instrumentRepository;
    this.userRepository = userRepository;
  }

  /** Alarm kuralını pasifleştirir. */
  @Override
  public void deactivateAlarm(Long alarmId, UUID currentUserId) {

    AlarmRule alarm =
        alarmRuleRepository
            .findById(alarmId)
            .orElseThrow(() -> new ResourceNotFoundException("Alarm not found: " + alarmId));

    if (!alarm.isOwnedBy(currentUserId)) {
      throw new AccessDeniedBusinessException("You are not allowed to deactivate this alarm");
    }

    alarm.deactivate();
    alarmRuleRepository.save(alarm);

    log.info("ALARM_DEACTIVATED alarmId={}, userId={}", alarmId, currentUserId);
  }

  /** Kullanıcı için yeni alarm kuralı oluşturur. */
  @Override
  public void createAlarm(
      UUID userId, Long instrumentId, AlarmCondition condition, BigDecimal threshold) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found"));

    Instrument instrument =
        instrumentRepository
            .findById(instrumentId)
            .orElseThrow(() -> new IllegalStateException("Instrument not found"));

    AlarmRule alarm = new AlarmRule(user, instrument, condition, threshold);

    alarmRuleRepository.save(alarm);
  }

  @Override
  @Transactional(readOnly = true)
  /** Kullanıcının alarm listesini döner. */
  public List<AlarmResponse> getUserAlarms(UUID userId) {

    List<AlarmRule> alarms =
        alarmRuleRepository.findByUserAndActiveTrue(
            userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found")));

    return alarms.stream()
        .map(
            alarm ->
                new AlarmResponse(
                    alarm.getId(),
                    alarm.getInstrument().getSymbol(),
                    alarm.getCondition(),
                    alarm.getThreshold(),
                    alarm.isActive(),
                    alarm.getCreatedAt()))
        .toList();
  }

  /** Güncel fiyata göre aktif alarm kurallarını değerlendirir. */
  @Override
  public List<AlarmRule> checkAlarms(Instrument instrument, InstrumentPrice latestPrice) {

    List<AlarmRule> activeAlarms = alarmRuleRepository.findByInstrumentAndActiveTrue(instrument);

    List<AlarmRule> triggeredAlarms = new ArrayList<>();

    for (AlarmRule alarm : activeAlarms) {
      Optional<AlarmEvaluator> evaluatorOpt = evaluatorFactory.getEvaluator(alarm.getCondition());
      if (evaluatorOpt.isEmpty()) {
        continue; // unsupported rule -> skip
      }

      AlarmEvaluator evaluator = evaluatorOpt.get();

      if (evaluator.evaluate(alarm, latestPrice)) {
        alarm.deactivate();
        alarmRuleRepository.save(alarm);
        triggeredAlarms.add(alarm);

        User owner = alarm.getUser();
        String userEmail = StringUtils.hasText(owner.getEmail()) ? owner.getEmail().trim() : null;
        String preferredLocale =
            StringUtils.hasText(owner.getPreferredLocale())
                ? owner.getPreferredLocale().trim()
                : "en";

        alarmHistoryRepository.save(
            new AlarmHistory(
                owner.getId(),
                alarm.getInstrument().getSymbol(),
                alarm.getCondition(),
                alarm.getThreshold(),
                latestPrice.getPrice()));

        alarmEventPublisher.publish(
            AlarmTriggeredEvent.of(
                alarm.getId(),
                owner.getId(),
                userEmail,
                preferredLocale,
                alarm.getInstrument().getSymbol(),
                alarm.getCondition(),
                alarm.getThreshold(),
                latestPrice.getPrice()));

        log.info(
            "ALARM_TRIGGERED user={}, instrument={}, price={}",
            owner.getId(),
            alarm.getInstrument().getSymbol(),
            latestPrice.getPrice());
      }
    }

    return triggeredAlarms;
  }

  @Override
  @Transactional(readOnly = true)
  /** ActiveAlarmsForUser sorgusunu döner. */
  public List<AlarmRule> getActiveAlarmsForUser(UUID userId) {

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found"));

    return alarmRuleRepository.findByUserAndActiveTrue(user);
  }
}
