package com.company.finance_api.service.impl;

import com.company.finance_api.domain.*;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.domain.enums.TransactionType;
import com.company.finance_api.event.TransactionExecutedEvent;
import com.company.finance_api.event.publisher.TransactionEventPublisher;
import com.company.finance_api.repository.*;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.PriceService;
import com.company.finance_api.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TradeServiceImpl implements TradeService {

    private final InstrumentRepository instrumentRepository;
    private final DemoBalanceRepository demoBalanceRepository;
    private final TransactionRepository transactionRepository;
    private final PriceService priceService;
    private final CurrentUserResolver currentUserResolver;
    private final UserRepository userRepository;
    private final TransactionEventPublisher transactionEventPublisher;

    @Override
    public Transaction buy(Long instrumentId, BigDecimal quantity) {

        UUID userId = currentUserResolver.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));


        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found"));


        InstrumentPrice price = priceService
                .getLatestPrice(instrument, PriceType.MARKET)
                .orElseThrow(() -> new IllegalStateException("Price not available"));

        DemoBalance balance =
                demoBalanceRepository.findByUser(user)
                        .orElseThrow(() -> new IllegalStateException("Demo balance not found"));

        BigDecimal totalCost =
                price.getPrice().multiply(quantity);

        if (balance.getBalance().compareTo(totalCost) < 0) {
            throw new IllegalStateException("Insufficient demo balance");
        }

        balance.decrease(totalCost);

        Transaction transaction =
                Transaction.buy(user, instrument, price.getPrice(), quantity);

        demoBalanceRepository.save(balance);

        Transaction saved = transactionRepository.save(transaction);

//  EVENT
        transactionEventPublisher.publish(
                TransactionExecutedEvent.of(
                        user.getId(),
                        instrument.getId(),
                        instrument.getSymbol(),
                        saved.getType(),
                        saved.getPrice(),
                        saved.getQuantity()
                )
        );

        return saved;
    }

    @Override
    public Transaction sell(Long instrumentId, BigDecimal quantity) {

        UUID userId = currentUserResolver.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found"));

        // 🔥 POSITION CHECK
        BigDecimal netQuantity = transactionRepository
                .findByUserAndInstrument(user, instrument)
                .stream()
                .map(tx -> tx.getType().name().equals("BUY")
                        ? tx.getQuantity()
                        : tx.getQuantity().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (netQuantity.compareTo(quantity) < 0) {
            throw new IllegalStateException("Insufficient position for sell");
        }

        InstrumentPrice price = priceService
                .getLatestPrice(instrument, PriceType.MARKET)
                .orElseThrow(() -> new IllegalStateException("Price not available"));

        DemoBalance balance = demoBalanceRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Demo balance not found"));

        BigDecimal totalGain = price.getPrice().multiply(quantity);

        balance.increase(totalGain);

        Transaction transaction =
                Transaction.sell(user, instrument, price.getPrice(), quantity);

        demoBalanceRepository.save(balance);
        Transaction saved = transactionRepository.save(transaction);

        transactionEventPublisher.publish(
                TransactionExecutedEvent.of(
                        user.getId(),
                        instrument.getId(),
                        instrument.getSymbol(),
                        saved.getType(),
                        saved.getPrice(),
                        saved.getQuantity()
                )
        );

        return saved;
    }

}