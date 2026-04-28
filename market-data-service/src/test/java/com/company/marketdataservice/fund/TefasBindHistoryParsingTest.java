package com.company.marketdataservice.fund;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TefasBindHistoryParsingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void latestNav_picksRowWithMaxTarihMillis() throws Exception {
        String json =
                """
                {
                  "data": [
                    {"FONKODU": "AFT", "FIYAT": "10.0", "TARIH": 1000},
                    {"FONKODU": "AFT", "FIYAT": "20.5", "TARIH": 2000}
                  ]
                }
                """;
        var o = TefasBindHistoryParsing.latestNav(mapper.readTree(json), "AFT");
        assertTrue(o.isPresent());
        assertEquals(new BigDecimal("20.5"), o.get().nav());
        assertEquals("AFT", o.get().fundCode());
        assertEquals(Instant.ofEpochMilli(2000), o.get().timestamp());
    }

    @Test
    void latestNav_emptyData_skips() throws Exception {
        String json = "{\"data\": []}";
        assertTrue(TefasBindHistoryParsing.latestNav(mapper.readTree(json), "AFT").isEmpty());
    }

    @Test
    void latestNav_missingData_skips() throws Exception {
        String json = "{}";
        assertTrue(TefasBindHistoryParsing.latestNav(mapper.readTree(json), "AFT").isEmpty());
    }
}
