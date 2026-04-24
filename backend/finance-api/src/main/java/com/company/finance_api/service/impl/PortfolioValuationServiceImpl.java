package com.company.finance_api.service.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.Transaction;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.InstrumentType;
import com.company.finance_api.domain.enums.TransactionType;
import com.company.finance_api.dto.PortfolioValuationAssetDto;
import com.company.finance_api.dto.PortfolioValuationResponse;
import com.company.finance_api.repository.TransactionRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.dto.InsightDto;
import com.company.finance_api.service.PortfolioInsightService;
import com.company.finance_api.service.PortfolioValuationService;
import com.company.finance_api.service.PriceService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioValuationServiceImpl implements PortfolioValuationService {

    private static final Set<Long> MISSING_PRICE_LOGGED = ConcurrentHashMap.newKeySet();

    private final TransactionRepository transactionRepository;
    private final PriceService priceService;
    private final CurrentUserResolver currentUserResolver;
    private final UserRepository userRepository;
    private final MeterRegistry meterRegistry;
    private final PortfolioInsightService portfolioInsightService;

    @Override
    public PortfolioValuationResponse getMyValuation() {
        meterRegistry.counter("portfolio_valuation_requests_total").increment();
        UUID userId = currentUserResolver.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        List<Transaction> transactions =
                transactionRepository.findByUserOrderByCreatedAtDesc(user);
        Map<Instrument, List<Transaction>> byInstrument =
                transactions.stream()
                        .collect(Collectors.groupingBy(Transaction::getInstrument));

        List<ValuationLine> lines = new ArrayList<>();
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
            var priceOpt = priceService.getLatestValuationPrice(instrument);
            if (priceOpt.isEmpty()) {
                meterRegistry.counter("portfolio_asset_missing_price_total").increment();
                if (MISSING_PRICE_LOGGED.add(instrument.getId())) {
                    log.warn("MISSING_PRICE_FOR_PORTFOLIO instrumentId={}", instrument.getId());
                }
                lines.add(new ValuationLine(instrument, BigDecimal.ZERO, BigDecimal.ZERO, false));
                continue;
            }
            InstrumentPrice currentPrice = priceOpt.get();
            BigDecimal currentValue = currentPrice.getPrice().multiply(quantity);
            BigDecimal pnl = currentValue.subtract(totalCost);
            lines.add(new ValuationLine(instrument, currentValue, pnl, true));
        }

        BigDecimal totalValue = lines.stream()
                .map(ValuationLine::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> segmentNumerator = new LinkedHashMap<>();
        for (ValuationLine line : lines) {
            String seg = segmentKey(line.instrument());
            segmentNumerator.merge(seg, line.currentValue(), BigDecimal::add);
        }

        Map<String, BigDecimal> segmentBreakdown = new LinkedHashMap<>();
        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<String, BigDecimal> e : segmentNumerator.entrySet()) {
                BigDecimal pct = e.getValue()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalValue, 6, RoundingMode.HALF_UP);
                segmentBreakdown.put(e.getKey(), pct);
            }
        }

        lines.sort(Comparator.comparing(line -> line.instrument().getId()));
        List<PortfolioValuationAssetDto> assets = new ArrayList<>();
        for (ValuationLine line : lines) {
            BigDecimal weight = totalValue.compareTo(BigDecimal.ZERO) > 0
                    ? line.currentValue()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalValue, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            assets.add(new PortfolioValuationAssetDto(
                    line.instrument().getId(),
                    line.currentValue(),
                    line.pnl(),
                    weight,
                    line.hasPrice()
            ));
        }

        List<InsightDto> insights = portfolioInsightService.analyze(assets, segmentBreakdown);
        return new PortfolioValuationResponse(totalValue, assets, segmentBreakdown, insights);
    }

    private static String segmentKey(Instrument instrument) {
        return switch (instrument.getType()) {
            case FX -> "FX";
            case CRYPTO -> "CRYPTO";
            case FUND -> "FUND";
            case STOCK -> "STOCK";
        };
    }

    private record ValuationLine(
            Instrument instrument,
            BigDecimal currentValue,
            BigDecimal pnl,
            boolean hasPrice
    ) {
    }
}
