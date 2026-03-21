package com.company.finance_api.service.impl;

import com.company.finance_api.domain.*;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.domain.enums.TransactionType;
import com.company.finance_api.dto.PortfolioPositionResponse;
import com.company.finance_api.dto.PortfolioSummaryResponse;
import com.company.finance_api.repository.TransactionRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.PortfolioService;
import com.company.finance_api.service.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final TransactionRepository transactionRepository;
    private final PriceService priceService;
    private final CurrentUserResolver currentUserResolver;
    private final UserRepository userRepository;

    @Override
    public List<PortfolioPositionResponse> getMyPortfolio() {
        UUID userId = currentUserResolver.getCurrentUserId();
        return getPortfolioByUserId(userId);
    }

    private List<PortfolioPositionResponse> getPortfolioByUserId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        List<Transaction> transactions =
                transactionRepository.findByUserOrderByCreatedAtDesc(user);
        Map<Instrument, List<Transaction>> byInstrument =
                transactions.stream()
                        .collect(Collectors.groupingBy(Transaction::getInstrument));
        List<PortfolioPositionResponse> positions = new ArrayList<>();
        for (Map.Entry<Instrument, List<Transaction>> entry : byInstrument.entrySet()) {
            Instrument instrument = entry.getKey();
            List<Transaction> txs = entry.getValue();
            BigDecimal quantity = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;
            for (Transaction tx : txs) {
                if (tx.getType() == TransactionType.BUY) {
                    quantity = quantity.add(tx.getQuantity());
                    totalCost = totalCost.add(tx.getTotalAmount());
                } else {
                    quantity = quantity.subtract(tx.getQuantity());
                    totalCost = totalCost.subtract(tx.getTotalAmount());
                }
            }
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal avgPrice = totalCost.divide(quantity, 6, RoundingMode.HALF_UP);
            InstrumentPrice currentPrice = priceService
                    .getLatestPrice(instrument, PriceType.MARKET)
                    .orElseThrow();
            BigDecimal currentValue = currentPrice.getPrice().multiply(quantity);
            BigDecimal unrealizedPnl = currentValue.subtract(totalCost);
            positions.add(new PortfolioPositionResponse(
                    instrument.getId(),
                    instrument.getSymbol(),
                    quantity,
                    avgPrice,
                    currentPrice.getPrice(),
                    totalCost,
                    currentValue,
                    unrealizedPnl
            ));
        }
        return positions;
    }

    @Override
    public PortfolioSummaryResponse getPortfolioSummary() {
        UUID userId = currentUserResolver.getCurrentUserId();
        return getPortfolioSummary(userId);
    }
    @Override
    public PortfolioSummaryResponse getPortfolioSummary(UUID userId) {
        List<PortfolioPositionResponse> positions = getPortfolioByUserId(userId);
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        for (PortfolioPositionResponse p : positions) {
            totalCost = totalCost.add(p.totalCost());
            totalValue = totalValue.add(p.currentValue());
        }
        BigDecimal unrealizedPnl = totalValue.subtract(totalCost);
        BigDecimal unrealizedPnlPercentage = BigDecimal.ZERO;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            unrealizedPnlPercentage = unrealizedPnl
                    .divide(totalCost, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return new PortfolioSummaryResponse(
                totalCost,
                totalValue,
                unrealizedPnl,
                unrealizedPnlPercentage
        );
    }
}