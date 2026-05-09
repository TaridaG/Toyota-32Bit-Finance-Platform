package com.company.finance_api.service.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.Transaction;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.TransactionType;
import com.company.finance_api.dto.PortfolioTradeFlowPointResponse;
import com.company.finance_api.dto.PortfolioTradeFlowResponse;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.repository.TransactionRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.CurrencyConversionService;
import com.company.finance_api.service.PortfolioTradeFlowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PortfolioTradeFlowServiceImpl implements PortfolioTradeFlowService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ExternalPortfolioRepository externalPortfolioRepository;
    private final CurrentUserResolver currentUserResolver;
    private final CurrencyConversionService currencyConversionService;

    public PortfolioTradeFlowServiceImpl(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            ExternalPortfolioRepository externalPortfolioRepository,
            CurrentUserResolver currentUserResolver,
            CurrencyConversionService currencyConversionService
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.externalPortfolioRepository = externalPortfolioRepository;
        this.currentUserResolver = currentUserResolver;
        this.currencyConversionService = currencyConversionService;
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioTradeFlowResponse getMyTradeFlow(String targetCurrency, Long portfolioId) {
        UUID userId = currentUserResolver.getCurrentUserId();
        String normalizedCurrency = currencyConversionService.normalizeCurrency(targetCurrency);
        if (portfolioId == null) {
            return new PortfolioTradeFlowResponse(normalizedCurrency, List.of());
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        ExternalPortfolio portfolio = externalPortfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElse(null);
        if (portfolio == null) {
            return new PortfolioTradeFlowResponse(normalizedCurrency, List.of());
        }
        List<Transaction> txs = transactionRepository.findByUserAndExternalPortfolioOrderByCreatedAtAsc(user, portfolio);
        List<PortfolioTradeFlowPointResponse> points = new ArrayList<>(txs.size());
        for (Transaction tx : txs) {
            Instrument ins = tx.getInstrument();
            BigDecimal notional = notionalInTarget(tx, ins, normalizedCurrency);
            if (notional == null) {
                continue;
            }
            BigDecimal signed = tx.getType() == TransactionType.BUY ? notional : notional.negate();
            points.add(new PortfolioTradeFlowPointResponse(tx.getId(), tx.getCreatedAt(), signed));
        }
        points.sort(Comparator
                .comparing(PortfolioTradeFlowPointResponse::createdAt)
                .thenComparingLong(PortfolioTradeFlowPointResponse::transactionId));
        return new PortfolioTradeFlowResponse(normalizedCurrency, points);
    }

    private BigDecimal notionalInTarget(Transaction tx, Instrument ins, String target) {
        String normalizedTarget = currencyConversionService.normalizeCurrency(target);
        if (tx.getInputAmount() != null && tx.getInputCurrency() != null && !tx.getInputCurrency().isBlank()) {
            String ic = currencyConversionService.normalizeCurrency(tx.getInputCurrency());
            if (ic.equals(normalizedTarget)) {
                return tx.getInputAmount().setScale(2, RoundingMode.HALF_UP);
            }
        }
        String insCur = resolveInstrumentCurrency(ins);
        BigDecimal conv = currencyConversionService.convert(tx.getTotalAmount(), insCur, normalizedTarget);
        if (conv == null) {
            conv = tx.getTotalAmount();
        }
        return conv.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveInstrumentCurrency(Instrument instrument) {
        if (instrument.getExchange() != null && "BIST".equalsIgnoreCase(instrument.getExchange().name())) {
            return "TRY";
        }
        String symbol = instrument.getSymbol() == null ? "" : instrument.getSymbol().toUpperCase(Locale.ROOT);
        if (symbol.endsWith("TRY")) {
            return "TRY";
        }
        if (symbol.endsWith("EUR")) {
            return "EUR";
        }
        return "USD";
    }
}
