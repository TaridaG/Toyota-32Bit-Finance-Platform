package com.company.marketdataservice.rates.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.marketdataservice.rates.infrastructure.persistence.TlDepositDailyIndexEntity;
import com.company.marketdataservice.rates.infrastructure.persistence.TlDepositWeeklyEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TlDepositDailyIndexSyncServiceTest {

  @Test
  void buildDailyRows_forwardFillsRateAndCompoundsDailyIndex() {
    TlDepositWeeklyEntity first = new TlDepositWeeklyEntity();
    first.setWeekStart(LocalDate.of(2025, 1, 1));
    first.setMaturityCode("MT04");
    first.setRatePercent(new BigDecimal("10.0000"));
    first.setSourceObservationDate(LocalDate.of(2025, 1, 1));
    first.setSourceProvider("TCMB_EVDS");

    TlDepositWeeklyEntity second = new TlDepositWeeklyEntity();
    second.setWeekStart(LocalDate.of(2025, 1, 6));
    second.setMaturityCode("MT04");
    second.setRatePercent(new BigDecimal("20.0000"));
    second.setSourceObservationDate(LocalDate.of(2025, 1, 3));
    second.setSourceProvider("TCMB_EVDS");

    List<TlDepositDailyIndexEntity> rows =
        TlDepositDailyIndexSyncService.buildDailyRows(
            "MT04", List.of(first, second), LocalDate.of(2025, 1, 4));

    assertEquals(4, rows.size());
    assertEquals(new BigDecimal("1.0000000000"), rows.get(0).getIndexValue());
    assertEquals(LocalDate.of(2025, 1, 1), rows.get(0).getDay());
    assertEquals(LocalDate.of(2025, 1, 3), rows.get(2).getSourceObservationDate());

    BigDecimal daily10 = BigDecimal.valueOf(Math.pow(1.10d, 1d / 365.25d)).setScale(10, java.math.RoundingMode.HALF_UP);
    BigDecimal daily20 = BigDecimal.valueOf(Math.pow(1.20d, 1d / 365.25d)).setScale(10, java.math.RoundingMode.HALF_UP);

    assertEquals(
        daily10,
        rows.get(1).getIndexValue());
    assertEquals(
        daily10.multiply(daily20).setScale(10, java.math.RoundingMode.HALF_UP),
        rows.get(2).getIndexValue());
    assertEquals(
        daily10.multiply(daily20).multiply(daily20).setScale(10, java.math.RoundingMode.HALF_UP),
        rows.get(3).getIndexValue());
  }
}
