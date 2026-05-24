package com.company.marketdataservice.rates.domain;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRateHistoryPointDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRatePointSourceQuality;
import com.company.marketdataservice.shared.provider.tcmb.BondEodPoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PolicyRateMonthlyAggregatorTest {

    @Test
    void lastObservationPerMonthThenForwardFill() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 4, 15);
        List<BondEodPoint> daily = List.of(
                new BondEodPoint(LocalDate.of(2024, 1, 5), bd("10")),
                new BondEodPoint(LocalDate.of(2024, 1, 20), bd("12")),
                new BondEodPoint(LocalDate.of(2024, 3, 2), bd("45"))
        );
        List<PolicyRateHistoryPointDto> out = PolicyRateMonthlyAggregator.aggregate(daily, start, end);
        assertEquals(4, out.size());
        assertEquals(LocalDate.of(2024, 1, 31), out.get(0).getDate());
        assertEquals(bd("12.00"), out.get(0).getValue());
        assertNull(out.get(0).getSourceQuality());
        assertEquals(LocalDate.of(2024, 2, 29), out.get(1).getDate());
        assertEquals(bd("12.00"), out.get(1).getValue());
        assertEquals(PolicyRatePointSourceQuality.PARTIAL, out.get(1).getSourceQuality());
        assertEquals(LocalDate.of(2024, 3, 31), out.get(2).getDate());
        assertEquals(bd("45.00"), out.get(2).getValue());
        assertNull(out.get(2).getSourceQuality());
        assertEquals(LocalDate.of(2024, 4, 15), out.get(3).getDate());
        assertEquals(bd("45.00"), out.get(3).getValue());
        assertEquals(PolicyRatePointSourceQuality.PARTIAL, out.get(3).getSourceQuality());
    }

    @Test
    void leadingGapUsesFirstObservedValue() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 3, 31);
        List<BondEodPoint> daily = List.of(new BondEodPoint(LocalDate.of(2024, 3, 1), bd("50")));
        List<PolicyRateHistoryPointDto> out = PolicyRateMonthlyAggregator.aggregate(daily, start, end);
        assertEquals(3, out.size());
        assertEquals(bd("50.00"), out.get(0).getValue());
        assertEquals(PolicyRatePointSourceQuality.PARTIAL, out.get(0).getSourceQuality());
        assertEquals(bd("50.00"), out.get(1).getValue());
        assertEquals(PolicyRatePointSourceQuality.PARTIAL, out.get(1).getSourceQuality());
        assertEquals(bd("50.00"), out.get(2).getValue());
        assertNull(out.get(2).getSourceQuality());
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}
