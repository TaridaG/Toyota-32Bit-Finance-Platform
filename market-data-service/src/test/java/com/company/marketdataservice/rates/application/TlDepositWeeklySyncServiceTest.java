package com.company.marketdataservice.rates.application;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TlDepositWeeklySyncServiceTest {

    @Test
    void maturitySuffixFromSeries_dottedAndUnderscored() {
        assertEquals("MT04", TlDepositWeeklySyncService.maturitySuffixFromSeries("TP.MT210AGS.TRY.MT04"));
        assertEquals("MT01", TlDepositWeeklySyncService.maturitySuffixFromSeries("TP_MT210AGS_TRY_MT01"));
    }
}
