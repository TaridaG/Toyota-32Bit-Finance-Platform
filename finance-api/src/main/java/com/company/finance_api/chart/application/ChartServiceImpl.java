package com.company.finance_api.chart.application;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.instrument.infrastructure.persistence.InstrumentRepository;
import com.company.finance_api.portfolio.infrastructure.persistence.TransactionRepository;
import com.company.finance_api.pricing.domain.InstrumentPrice;
import com.company.finance_api.pricing.domain.enums.PriceType;
import com.company.finance_api.pricing.infrastructure.persistence.InstrumentPriceRepository;
import com.company.finance_api.alarm.infrastructure.http.dto.AlarmLineResponse;
import com.company.finance_api.alarm.infrastructure.persistence.AlarmRuleRepository;
import com.company.finance_api.chart.infrastructure.http.dto.CandlestickResponse;
import com.company.finance_api.chart.infrastructure.http.dto.TradeMarkerResponse;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** ChartServiceImpl iş mantığını uygular (chart service). */
@Service
@RequiredArgsConstructor
public class ChartServiceImpl implements ChartService {

  private final InstrumentPriceRepository priceRepository;
  private final TransactionRepository transactionRepository;
  private final AlarmRuleRepository alarmRuleRepository;
  private final InstrumentRepository instrumentRepository;
  private final UserRepository userRepository;
  private final CurrentUserResolver currentUserResolver;

  /** Candlesticks sorgusunu döner. */
  @Override
  public List<CandlestickResponse> getCandlesticks(
      Long instrumentId, Instant from, Instant to, PriceType priceType) {

    Instrument instrument = instrumentRepository.findById(instrumentId).orElseThrow();

    List<InstrumentPrice> prices =
        priceRepository.findByInstrumentAndPriceTypeAndTimestampBetweenOrderByTimestampAsc(
            instrument, priceType, from, to);

    return prices.stream()
        .map(
            p ->
                new CandlestickResponse(
                    p.getTimestamp(), p.getPrice(), p.getPrice(), p.getPrice(), p.getPrice()))
        .toList();
  }

  /** MyTrades sorgusunu döner. */
  @Override
  public List<TradeMarkerResponse> getMyTrades(Long instrumentId) {

    UUID userId = currentUserResolver.getCurrentUserId();
    User user = userRepository.findById(userId).orElseThrow();
    Instrument instrument = instrumentRepository.findById(instrumentId).orElseThrow();

    return transactionRepository.findByUserOrderByCreatedAtDesc(user).stream()
        .filter(tx -> tx.getInstrument().equals(instrument))
        .map(
            tx ->
                new TradeMarkerResponse(
                    tx.getCreatedAt(), tx.getType(), tx.getPrice(), tx.getQuantity()))
        .toList();
  }

  /** MyAlarms sorgusunu döner. */
  @Override
  public List<AlarmLineResponse> getMyAlarms(Long instrumentId) {

    UUID userId = currentUserResolver.getCurrentUserId();
    User user = userRepository.findById(userId).orElseThrow();
    Instrument instrument = instrumentRepository.findById(instrumentId).orElseThrow();

    return alarmRuleRepository.findByUserAndInstrumentAndActiveTrue(user, instrument).stream()
        .map(rule -> new AlarmLineResponse(rule.getCondition(), rule.getThreshold()))
        .collect(Collectors.toList());
  }
}
