package com.company.marketdataservice.rates.application;
import com.company.marketdataservice.rates.domain.PolicyRateLatestResolver;
import com.company.marketdataservice.rates.infrastructure.persistence.TcmbPolicyRateWeeklyEntity;
import com.company.marketdataservice.rates.infrastructure.persistence.TcmbPolicyRateWeeklyRepository;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRateHistoryPointDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRateHistoryResponseDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRateLatestDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * `makro oran` application katmanı use-case servisi.
 */
@Service
public class PolicyRateHistoryService {

    private static final ZoneId TR = ZoneId.of("Europe/Istanbul");

    private final TcmbPolicyRateWeeklyRepository weeklyRepository;
    private final PolicyRateWeeklySyncService policyRateWeeklySyncService;

    public PolicyRateHistoryService(
            TcmbPolicyRateWeeklyRepository weeklyRepository,
            PolicyRateWeeklySyncService policyRateWeeklySyncService
    ) {
        this.weeklyRepository = weeklyRepository;
        this.policyRateWeeklySyncService = policyRateWeeklySyncService;
    }

    /**
     * Latest stored week (for UI). Triggers a one-shot async EVDS sync when the table is still empty.
     */
    public PolicyRateLatestDto loadLatest() {
        policyRateWeeklySyncService.requestInitialSyncIfEmptyAsync();
        return buildLatestFromDb();
    }

    private PolicyRateLatestDto buildLatestFromDb() {
        PolicyRateLatestDto dto = new PolicyRateLatestDto();
        Optional<TcmbPolicyRateWeeklyEntity> top = weeklyRepository.findTopByOrderByWeekStartDesc();
        if (top.isEmpty()) {
            return dto;
        }
        TcmbPolicyRateWeeklyEntity cur = top.get();
        dto.setValue(cur.getRatePercent().setScale(2, RoundingMode.HALF_UP));
        LocalDate todayTr = LocalDate.now(TR);
        LocalDate decision = cur.getSourceObservationDate() != null
                ? cur.getSourceObservationDate()
                : cur.getWeekStart().plusDays(6);
        if (decision.isAfter(todayTr)) {
            decision = todayTr;
        }
        dto.setDecisionDate(decision);

        Optional<TcmbPolicyRateWeeklyEntity> prior = weeklyRepository.findFirstByWeekStartLessThanOrderByWeekStartDesc(cur.getWeekStart());
        if (prior.isEmpty()) {
            dto.setChangeVsPrior(PolicyRateLatestResolver.CHANGE_UNCHANGED);
            return dto;
        }
        BigDecimal prevVal = prior.get().getRatePercent();
        int c = cur.getRatePercent().compareTo(prevVal);
        if (c > 0) {
            dto.setChangeVsPrior(PolicyRateLatestResolver.CHANGE_UP);
        } else if (c < 0) {
            dto.setChangeVsPrior(PolicyRateLatestResolver.CHANGE_DOWN);
        } else {
            dto.setChangeVsPrior(PolicyRateLatestResolver.CHANGE_UNCHANGED);
        }
        return dto;
    }

    /**
     * Veriyi yükler.
         * @return işlem sonucu
         */
    public PolicyRateHistoryResponseDto loadFiveYearWeeklyFromDb() {
        PolicyRateHistoryResponseDto out = new PolicyRateHistoryResponseDto();
        out.setFrequency("WEEKLY");
        LocalDate rangeEnd = LocalDate.now(TR);
        LocalDate rangeStart = rangeEnd.minusYears(5).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<TcmbPolicyRateWeeklyEntity> rows = weeklyRepository.findByWeekStartBetweenOrderByWeekStartAsc(rangeStart, rangeEnd);
        List<PolicyRateHistoryPointDto> points = new ArrayList<>(rows.size());
        for (TcmbPolicyRateWeeklyEntity r : rows) {
            LocalDate weekEndSunday = r.getWeekStart().plusDays(6);
            points.add(new PolicyRateHistoryPointDto(
                    weekEndSunday,
                    r.getRatePercent().setScale(2, RoundingMode.HALF_UP),
                    null
            ));
        }
        out.setPoints(points);
        return out;
    }
}
