package com.company.marketdataservice.provider.tcmb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression: EVDS rows can include {@code TP_KANUNI_FAIZ_ORAN} next to the policy series. If the policy cell is blank,
 * we must not read kanuni (thousands-scale %) as the policy rate.
 */
class TcmbBondEvdsClientParseTest {

    private static final ObjectMapper OM = new ObjectMapper();

    @Test
    void blankPolicyCellDoesNotFallBackToKanuni() throws Exception {
        String json =
                "{\"Tarih\":\"01-01-2024\",\"TP_BISPOLFAIZ_TUR\":\"\",\"TP_KANUNI_FAIZ_ORAN\":\"3.683,83\"}";
        assertNull(TcmbBondEvdsClient.parseBondValue(OM.readTree(json), "TP_BISPOLFAIZ_TUR"));
    }

    @Test
    void readsUnderscoredSeriesColumn() throws Exception {
        String json = "{\"Tarih\":\"01-01-2024\",\"TP_BISPOLFAIZ_TUR\":\"37,50\"}";
        assertEquals(new BigDecimal("37.50"), TcmbBondEvdsClient.parseBondValue(OM.readTree(json), "TP_BISPOLFAIZ_TUR"));
    }
}
