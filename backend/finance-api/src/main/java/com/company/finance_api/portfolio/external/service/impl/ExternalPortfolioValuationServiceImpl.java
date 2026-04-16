package com.company.finance_api.portfolio.external.service.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.portfolio.external.domain.ExternalPositionLot;
import com.company.finance_api.portfolio.external.dto.ExternalPortfolioAllocationResponse;
import com.company.finance_api.portfolio.external.dto.ExternalPortfolioSummaryResponse;
import com.company.finance_api.portfolio.external.dto.ExternalPositionSummary;
import com.company.finance_api.portfolio.external.repository.ExternalPositionLotRepository;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.portfolio.external.service.ExternalPortfolioValuationService;
import com.company.finance_api.portfolio.valuation.InstrumentPriceProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExternalPortfolioValuationServiceImpl implements ExternalPortfolioValuationService {

    private final ExternalPortfolioRepository portfolioRepository;
    private final ExternalPositionLotRepository lotRepository;
    private final InstrumentPriceProvider priceProvider;

    @Override
    public ExternalPortfolioSummaryResponse calculateSummary(UUID userId, Long portfolioId) {

        portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        List<ExternalPositionLot> lots =
                lotRepository.findAllByPortfolioIdAndDeletedFalseOrderByAcquiredAtAsc(portfolioId);

        Map<Long, List<ExternalPositionLot>> grouped =
                lots.stream().collect(Collectors.groupingBy(l -> l.getInstrument().getId()));

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalMarket = BigDecimal.ZERO;

        List<ExternalPositionSummary> summaries = new ArrayList<>();

        for (Map.Entry<Long, List<ExternalPositionLot>> entry : grouped.entrySet()) {

            List<ExternalPositionLot> instrumentLots = entry.getValue();
            Instrument instrument = instrumentLots.get(0).getInstrument();

            BigDecimal quantity = BigDecimal.ZERO;
            BigDecimal cost = BigDecimal.ZERO;

            for (ExternalPositionLot lot : instrumentLots) {
                quantity = quantity.add(lot.getQuantity());
                cost = cost.add(lot.totalCost());
            }

            BigDecimal avgCost = cost.divide(quantity, 8, RoundingMode.HALF_UP);

            BigDecimal currentPrice = priceProvider.getCurrentPrice(instrument.getId());

            BigDecimal marketValue = currentPrice.multiply(quantity);

            BigDecimal pnl = marketValue.subtract(cost);

            BigDecimal pnlPct = cost.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : pnl.divide(cost, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            totalCost = totalCost.add(cost);
            totalMarket = totalMarket.add(marketValue);

            summaries.add(
                    ExternalPositionSummary.builder()
                            .instrumentId(instrument.getId())
                            .symbol(instrument.getSymbol())
                            .quantity(quantity)
                            .avgCost(avgCost)
                            .currentPrice(currentPrice)
                            .marketValue(marketValue)
                            .pnl(pnl)
                            .pnlPercentage(pnlPct)
                            .build()
            );
        }

        BigDecimal totalPnL = totalMarket.subtract(totalCost);

        BigDecimal totalPnLPct = totalCost.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalPnL.divide(totalCost, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return ExternalPortfolioSummaryResponse.builder()
                .totalCost(totalCost)
                .totalMarketValue(totalMarket)
                .totalPnL(totalPnL)
                .totalPnLPercentage(totalPnLPct)
                .positions(summaries)
                .build();
    }

    @Override
    public List<ExternalPortfolioAllocationResponse> calculateAllocation(UUID userId, Long portfolioId) {

        ExternalPortfolioSummaryResponse summary = calculateSummary(userId, portfolioId);

        BigDecimal totalMarket = summary.getTotalMarketValue();

        return summary.getPositions().stream()
                .map(p -> ExternalPortfolioAllocationResponse.builder()
                        .symbol(p.getSymbol())
                        .marketValue(p.getMarketValue())
                        .percentage(
                                totalMarket.compareTo(BigDecimal.ZERO) == 0
                                        ? BigDecimal.ZERO
                                        : p.getMarketValue()
                                        .divide(totalMarket, 6, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100))
                        )
                        .build()
                )
                .toList();
    }
}

