package com.company.finance_api.service.impl;

import com.company.finance_api.dto.InsightDto;
import com.company.finance_api.dto.InsightSeverity;
import com.company.finance_api.dto.PortfolioValuationAssetDto;
import com.company.finance_api.service.PortfolioInsightService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** PortfolioInsightServiceImpl iş mantığını uygular (portfolio insight service). */
@Service
@RequiredArgsConstructor
public class PortfolioInsightServiceImpl implements PortfolioInsightService {

  private static final BigDecimal FIFTY = BigDecimal.valueOf(50);
  private static final BigDecimal SEVENTY = BigDecimal.valueOf(70);

  private final MeterRegistry meterRegistry;

  /** analyze işlemini gerçekleştirir. */
  @Override
  public List<InsightDto> analyze(
      List<PortfolioValuationAssetDto> assets, Map<String, BigDecimal> segmentBreakdownPercent) {
    List<InsightDto> insights = new ArrayList<>();
    if (assets.isEmpty()) {
      insights.add(
          new InsightDto("PORTFOLIO_EMPTY", "No open positions.", InsightSeverity.LOW, Map.of()));
      recordInsightMetrics(insights);
      return insights;
    }
    long missingPriceCount = assets.stream().filter(a -> !a.hasPrice()).count();
    if (missingPriceCount > 0) {
      insights.add(
          new InsightDto(
              "INCOMPLETE_DATA",
              missingPriceCount + " position(s) lack a valuation price.",
              InsightSeverity.MEDIUM,
              Map.of("missingCount", missingPriceCount)));
    }
    assets.stream()
        .filter(a -> a.weight().compareTo(FIFTY) > 0)
        .max(
            Comparator.comparing(PortfolioValuationAssetDto::weight)
                .thenComparing(PortfolioValuationAssetDto::instrumentId))
        .ifPresent(
            a ->
                insights.add(
                    new InsightDto(
                        "HIGH_CONCENTRATION",
                        "At least one position exceeds 50% portfolio weight.",
                        InsightSeverity.HIGH,
                        Map.of(
                            "instrumentId", a.instrumentId(),
                            "weight", a.weight()))));
    for (Map.Entry<String, BigDecimal> e : segmentBreakdownPercent.entrySet()) {
      if (e.getValue().compareTo(SEVENTY) > 0) {
        insights.add(
            new InsightDto(
                "SEGMENT_OVEREXPOSURE",
                "Segment "
                    + e.getKey()
                    + " represents "
                    + e.getValue().stripTrailingZeros().toPlainString()
                    + "% of portfolio value.",
                InsightSeverity.HIGH,
                Map.of(
                    "segment", e.getKey(),
                    "percent", e.getValue())));
      }
    }
    Optional<PortfolioValuationAssetDto> largest =
        assets.stream()
            .max(
                Comparator.comparing(PortfolioValuationAssetDto::weight)
                    .thenComparing(PortfolioValuationAssetDto::instrumentId));
    largest
        .filter(a -> a.weight().compareTo(BigDecimal.ZERO) > 0)
        .ifPresent(
            a ->
                insights.add(
                    new InsightDto(
                        "LARGEST_POSITION",
                        "Largest position instrumentId="
                            + a.instrumentId()
                            + " weight="
                            + a.weight().stripTrailingZeros().toPlainString()
                            + "%.",
                        InsightSeverity.LOW,
                        Map.of(
                            "instrumentId", a.instrumentId(),
                            "weight", a.weight()))));
    List<PortfolioValuationAssetDto> priced =
        assets.stream().filter(PortfolioValuationAssetDto::hasPrice).toList();
    if (!priced.isEmpty()) {
      PortfolioValuationAssetDto gainer =
          priced.stream()
              .max(
                  Comparator.comparing(PortfolioValuationAssetDto::pnl)
                      .thenComparing(PortfolioValuationAssetDto::instrumentId))
              .orElseThrow();
      insights.add(
          new InsightDto(
              "TOP_GAINER",
              "Top PnL instrumentId="
                  + gainer.instrumentId()
                  + " pnl="
                  + gainer.pnl().stripTrailingZeros().toPlainString()
                  + ".",
              InsightSeverity.LOW,
              Map.of(
                  "instrumentId", gainer.instrumentId(),
                  "pnl", gainer.pnl())));
      if (assets.size() > 1) {
        PortfolioValuationAssetDto loser =
            priced.stream()
                .min(
                    Comparator.comparing(PortfolioValuationAssetDto::pnl)
                        .thenComparing(PortfolioValuationAssetDto::instrumentId))
                .orElseThrow();
        insights.add(
            new InsightDto(
                "TOP_LOSER",
                "Weakest PnL instrumentId="
                    + loser.instrumentId()
                    + " pnl="
                    + loser.pnl().stripTrailingZeros().toPlainString()
                    + ".",
                InsightSeverity.LOW,
                Map.of(
                    "instrumentId", loser.instrumentId(),
                    "pnl", loser.pnl())));
      }
    }
    recordInsightMetrics(insights);
    return insights;
  }

  private void recordInsightMetrics(List<InsightDto> insights) {
    for (InsightDto insight : insights) {
      meterRegistry
          .counter("portfolio_insight_generated_total", Tags.of("type", insight.type()))
          .increment();
    }
  }
}
