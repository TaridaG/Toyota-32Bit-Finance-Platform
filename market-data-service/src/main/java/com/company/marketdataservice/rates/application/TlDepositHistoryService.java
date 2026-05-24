package com.company.marketdataservice.rates.application;
import com.company.marketdataservice.rates.domain.PolicyRateLatestResolver;
import com.company.marketdataservice.rates.infrastructure.persistence.TlDepositWeeklyEntity;
import com.company.marketdataservice.rates.infrastructure.persistence.TlDepositWeeklyRepository;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRateHistoryPointDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRateHistoryResponseDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.TlDepositLatestDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;


/**
 * `makro oran` application katmanı use-case servisi.
 */
@Service
public class TlDepositHistoryService {

    private static final ZoneId TR = ZoneId.of("Europe/Istanbul");

    private final TlDepositWeeklyRepository tlDepositWeeklyRepository;
    private final TlDepositWeeklySyncService tlDepositWeeklySyncService;

    public TlDepositHistoryService(
            TlDepositWeeklyRepository tlDepositWeeklyRepository,
            TlDepositWeeklySyncService tlDepositWeeklySyncService
    ) {
        this.tlDepositWeeklyRepository = tlDepositWeeklyRepository;
        this.tlDepositWeeklySyncService = tlDepositWeeklySyncService;
    }

    /**
     * Veriyi yükler.
         * @param maturityParam girdi parametresi
         * @return işlem sonucu
         */
    public TlDepositLatestDto loadLatest(String maturityParam) {
        String maturity = resolveMaturity(maturityParam);
        tlDepositWeeklySyncService.requestInitialSyncIfEmptyAsync();
        return buildLatestFromDb(maturity);
    }

    private String resolveMaturity(String raw) {
        Set<String> allowed = tlDepositWeeklySyncService.configuredMaturitySuffixes();
        if (!StringUtils.hasText(raw)) {
            String preferred = tlDepositWeeklySyncService.displayMaturityCode();
            if (allowed.isEmpty()) {
                return preferred;
            }
            if (allowed.contains(preferred)) {
                return preferred;
            }
            return allowed.iterator().next();
        }
        String m = raw.trim().toUpperCase(Locale.ROOT);
        if (allowed.isEmpty() || !allowed.contains(m)) {
            throw new IllegalArgumentException("Unsupported maturity (allowed: " + allowed + ")");
        }
        return m;
    }

    private TlDepositLatestDto buildLatestFromDb(String maturity) {
        TlDepositLatestDto dto = new TlDepositLatestDto();
        dto.setMaturityCode(maturity);
        Optional<TlDepositWeeklyEntity> top = tlDepositWeeklyRepository.findTopByMaturityCodeOrderByWeekStartDesc(maturity);
        if (top.isEmpty()) {
            return dto;
        }
        TlDepositWeeklyEntity cur = top.get();
        dto.setValue(cur.getRatePercent().setScale(2, RoundingMode.HALF_UP));

        LocalDate todayTr = LocalDate.now(TR);
        LocalDate asOf = cur.getSourceObservationDate() != null
                ? cur.getSourceObservationDate()
                : cur.getWeekStart().plusDays(6);
        if (asOf.isAfter(todayTr)) {
            asOf = todayTr;
        }
        dto.setAsOfDate(asOf);

        Optional<TlDepositWeeklyEntity> prior = tlDepositWeeklyRepository
                .findFirstByMaturityCodeAndWeekStartLessThanOrderByWeekStartDesc(maturity, cur.getWeekStart());
        if (prior.isEmpty()) {
            dto.setChangeVsPrior(PolicyRateLatestResolver.CHANGE_UNCHANGED);
            return dto;
        }
        BigDecimal prevVal = prior.get().getRatePercent().setScale(2, RoundingMode.HALF_UP);
        dto.setPreviousValue(prevVal);
        int c = cur.getRatePercent().compareTo(prior.get().getRatePercent());
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
         * @param maturityParam girdi parametresi
         * @return işlem sonucu
         */
    public PolicyRateHistoryResponseDto loadFiveYearWeeklyFromDb(String maturityParam) {
        String maturity = resolveMaturity(maturityParam);
        PolicyRateHistoryResponseDto out = new PolicyRateHistoryResponseDto();
        out.setFrequency("WEEKLY");
        LocalDate rangeEnd = LocalDate.now(TR);
        LocalDate rangeStart = rangeEnd.minusYears(5).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<TlDepositWeeklyEntity> rows = tlDepositWeeklyRepository.findByMaturityCodeAndWeekStartBetweenOrderByWeekStartAsc(
                maturity,
                rangeStart,
                rangeEnd
        );
        List<PolicyRateHistoryPointDto> points = new ArrayList<>(rows.size());
        for (TlDepositWeeklyEntity r : rows) {
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
