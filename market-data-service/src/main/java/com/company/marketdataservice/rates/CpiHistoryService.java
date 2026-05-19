package com.company.marketdataservice.rates;

import com.company.marketdataservice.dto.CpiLatestDto;
import com.company.marketdataservice.dto.PolicyRateHistoryPointDto;
import com.company.marketdataservice.dto.PolicyRateHistoryResponseDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CpiHistoryService {

    private static final ZoneId TR = ZoneId.of("Europe/Istanbul");

    private final MdsCpiMonthlyRepository cpiRepository;
    private final CpiMonthlySyncService cpiMonthlySyncService;

    public CpiHistoryService(MdsCpiMonthlyRepository cpiRepository, CpiMonthlySyncService cpiMonthlySyncService) {
        this.cpiRepository = cpiRepository;
        this.cpiMonthlySyncService = cpiMonthlySyncService;
    }

    public CpiLatestDto loadLatest(CpiMetric metric) {
        cpiMonthlySyncService.requestInitialSyncIfEmptyAsync();
        CpiLatestDto dto = new CpiLatestDto();
        dto.setMetric(metric.name());
        dto.setUnit(metric == CpiMetric.INDEX ? "INDEX" : "PERCENT");

        Optional<MdsCpiMonthlyEntity> top = cpiRepository.findTopByMetricOrderByMonthStartDesc(metric);
        if (top.isEmpty()) {
            return dto;
        }
        MdsCpiMonthlyEntity cur = top.get();
        int scale = metric == CpiMetric.INDEX ? 2 : 2;
        dto.setValue(cur.getValue().setScale(scale, RoundingMode.HALF_UP));
        dto.setObservationMonth(cur.getMonthStart());

        Optional<MdsCpiMonthlyEntity> prior = cpiRepository.findFirstByMetricAndMonthStartLessThanOrderByMonthStartDesc(
                metric,
                cur.getMonthStart()
        );
        prior.ifPresent(p -> dto.setDeltaVsPriorMonth(
                cur.getValue().subtract(p.getValue()).setScale(2, RoundingMode.HALF_UP)
        ));
        return dto;
    }

    public PolicyRateHistoryResponseDto loadFiveYearMonthlyFromDb(CpiMetric metric) {
        PolicyRateHistoryResponseDto out = new PolicyRateHistoryResponseDto();
        out.setFrequency("MONTHLY");
        out.setUnit(metric == CpiMetric.INDEX ? "INDEX" : "PERCENT");
        out.setSourceProvider("TCMB_EVDS");

        LocalDate rangeEnd = LocalDate.now(TR);
        LocalDate rangeStart = rangeEnd.minusYears(5).withDayOfMonth(1);
        List<MdsCpiMonthlyEntity> rows = cpiRepository.findByMetricAndMonthStartBetweenOrderByMonthStartAsc(
                metric,
                rangeStart,
                rangeEnd
        );
        int scale = metric == CpiMetric.INDEX ? 2 : 2;
        List<PolicyRateHistoryPointDto> points = new ArrayList<>(rows.size());
        for (MdsCpiMonthlyEntity r : rows) {
            LocalDate chartDate = r.getMonthStart().withDayOfMonth(r.getMonthStart().lengthOfMonth());
            points.add(new PolicyRateHistoryPointDto(
                    chartDate,
                    r.getValue().setScale(scale, RoundingMode.HALF_UP),
                    null
            ));
        }
        out.setPoints(points);
        return out;
    }
}
