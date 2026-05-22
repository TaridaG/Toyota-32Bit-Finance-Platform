package com.company.marketdataservice.rates.bank;

import com.company.marketdataservice.dto.BankRatesRowDto;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DovizBankRatesHtmlParserTest {

    @Test
    void parsesUsdBankRatesFromFixture() throws Exception {
        String html =
                Files.readString(
                        Path.of("src/test/resources/doviz-usd-bank-rates-snippet.html"),
                        StandardCharsets.UTF_8);

        DovizBankRatesHtmlParser.ParsedBankRatesTable table = DovizBankRatesHtmlParser.parse(html);

        assertEquals("Amerikan Doları Banka Kurları", table.title());
        assertTrue(table.rows().size() >= 2);

        BankRatesRowDto first = table.rows().get(0);
        assertEquals("kapalicarsi", first.getSlug());
        assertEquals("Kapalıçarşı", first.getName());
        assertEquals(new BigDecimal("45.5300"), first.getBuy());
        assertEquals(new BigDecimal("45.5400"), first.getSell());
        assertEquals(new BigDecimal("0.0100"), first.getSpread());
        assertEquals(new BigDecimal("0.02"), first.getSpreadPercent());
        assertTrue(first.isBestBuy());
        assertTrue(first.isBestSell());
        assertTrue(first.isBestSpreadMin());

        BankRatesRowDto second = table.rows().get(1);
        assertTrue(second.isBestSpreadMax());
        assertTrue(first.isBestSpreadPctMin());
        assertTrue(second.isBestSpreadPctMax());
    }
}
