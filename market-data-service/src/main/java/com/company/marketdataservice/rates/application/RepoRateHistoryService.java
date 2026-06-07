package com.company.marketdataservice.rates.application;

import com.company.marketdataservice.bootstrap.config.MarketEvdsProperties;
import com.company.marketdataservice.rates.domain.PolicyRateWeeklyAggregator;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRateHistoryPointDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRateHistoryResponseDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.RepoRateLatestDto;
import com.company.marketdataservice.rates.infrastructure.persistence.TcmbRepoRatePointEntity;
import com.company.marketdataservice.rates.infrastructure.persistence.TcmbRepoRatePointRepository;
import com.company.marketdataservice.shared.provider.tcmb.BondEodPoint;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TCMB repo faizi güncel değer ve haftalık geçmiş sorgularını sunan `makro oran` application katmanı use-case servisi.
 */
@Service
public class RepoRateHistoryService {

    private static final ZoneId TR = ZoneId.of("Europe/Istanbul");

    private final TcmbRepoRatePointRepository pointRepository;
    private final RepoRateSyncService repoRateSyncService;
    private final MarketEvdsProperties evdsProperties;

    public RepoRateHistoryService(
            TcmbRepoRatePointRepository pointRepository,
            RepoRateSyncService repoRateSyncService,
            MarketEvdsProperties evdsProperties
    ) {
        this.pointRepository = pointRepository;
        this.repoRateSyncService = repoRateSyncService;
        this.evdsProperties = evdsProperties;
    }

    public RepoRateLatestDto loadLatest() {
        repoRateSyncService.requestInitialSyncIfEmptyAsync();
        return buildLatestFromDb();
    }

    public PolicyRateHistoryResponseDto loadFiveYearWeeklyFromDb() {
        PolicyRateHistoryResponseDto out = new PolicyRateHistoryResponseDto();
        out.setSymbol("TR_REPO_RATE");
        out.setName("TCMB 1 Hafta Repo Faizi");
        out.setFrequency("WEEKLY");

        LocalDate rangeEnd = LocalDate.now(TR);
        LocalDate rangeStart = rangeEnd.minusYears(5).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<TcmbRepoRatePointEntity> dailyRows =
                pointRepository.findByObservationDateBetweenOrderByObservationDateAsc(rangeStart, rangeEnd);
        if (dailyRows.isEmpty()) {
            out.setPoints(List.of());
            return out;
        }

        List<BondEodPoint> dailyPoints = dailyRows.stream()
                .map(r -> new BondEodPoint(r.getObservationDate(), r.getRatePercent()))
                .toList();
        Map<LocalDate, PolicyRateWeeklyAggregator.WeekPolicyPick> byWeek =
                PolicyRateWeeklyAggregator.lastPickByWeekMonday(dailyPoints, rangeStart, rangeEnd);

        List<PolicyRateHistoryPointDto> points = new ArrayList<>(byWeek.size());
        byWeek.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    LocalDate weekEndSunday = e.getKey().plusDays(6);
                    points.add(new PolicyRateHistoryPointDto(
                            weekEndSunday,
                            e.getValue().ratePercent().setScale(2, RoundingMode.HALF_UP),
                            null
                    ));
                });
        out.setPoints(points);
        return out;
    }

    private RepoRateLatestDto buildLatestFromDb() {
        RepoRateLatestDto dto = new RepoRateLatestDto();
        dto.setEvdsSeries(evdsProperties.getRepoRateSeries());

        Optional<TcmbRepoRatePointEntity> top = pointRepository.findTopByOrderByObservationDateDesc();
        if (top.isEmpty()) {
            return dto;
        }

        TcmbRepoRatePointEntity cur = top.get();
        dto.setValue(cur.getRatePercent().setScale(2, RoundingMode.HALF_UP));
        LocalDate todayTr = LocalDate.now(TR);
        LocalDate observation = cur.getObservationDate();
        if (observation.isAfter(todayTr)) {
            observation = todayTr;
        }
        dto.setObservationDate(observation);

        Optional<TcmbRepoRatePointEntity> prior =
                pointRepository.findFirstByObservationDateLessThanOrderByObservationDateDesc(observation);
        if (prior.isEmpty()) {
            return dto;
        }

        BigDecimal diffPercent = cur.getRatePercent().subtract(prior.get().getRatePercent());
        dto.setChange1dBasisPoints(diffPercent.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue());
        return dto;
    }
}
