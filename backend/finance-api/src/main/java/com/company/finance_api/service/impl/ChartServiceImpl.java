package com.company.finance_api.service.impl;

import com.company.finance_api.domain.*;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.dto.*;
import com.company.finance_api.repository.*;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.ChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChartServiceImpl implements ChartService {

    private final InstrumentPriceRepository priceRepository;
    private final TransactionRepository transactionRepository;
    private final AlarmRuleRepository alarmRuleRepository;
    private final InstrumentRepository instrumentRepository;
    private final UserRepository userRepository;
    private final CurrentUserResolver currentUserResolver;

    @Override
    public List<CandlestickResponse> getCandlesticks(
            Long instrumentId,
            Instant from,
            Instant to,
            PriceType priceType
    ) {

        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow();

        List<InstrumentPrice> prices =
                priceRepository.findByInstrumentAndPriceTypeAndTimestampBetweenOrderByTimestampAsc(
                        instrument,
                        priceType,
                        from,
                        to
                );

        return prices.stream()
                .map(p ->
                        new CandlestickResponse(
                                p.getTimestamp(),
                                p.getPrice(),
                                p.getPrice(),
                                p.getPrice(),
                                p.getPrice()
                        )
                )
                .toList();
    }

    @Override
    public List<TradeMarkerResponse> getMyTrades(Long instrumentId) {

        UUID userId = currentUserResolver.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow();
        Instrument instrument = instrumentRepository.findById(instrumentId).orElseThrow();

        return transactionRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .filter(tx -> tx.getInstrument().equals(instrument))
                .map(tx ->
                        new TradeMarkerResponse(
                                tx.getCreatedAt(),
                                tx.getType(),
                                tx.getPrice(),
                                tx.getQuantity()
                        )
                )
                .toList();
    }

    @Override
    public List<AlarmLineResponse> getMyAlarms(Long instrumentId) {

        UUID userId = currentUserResolver.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow();
        Instrument instrument = instrumentRepository.findById(instrumentId).orElseThrow();

        return alarmRuleRepository
                .findByUserAndInstrumentAndActiveTrue(user, instrument)
                .stream()
                .map(rule ->
                        new AlarmLineResponse(
                                rule.getCondition(),
                                rule.getThreshold()
                        )
                )
                .collect(Collectors.toList());
    }
}