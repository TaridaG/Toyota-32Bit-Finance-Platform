package com.company.finance_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.domain.EurobondInstrument;
import com.company.finance_api.domain.EurobondQuote;
import com.company.finance_api.dto.market.eurobond.EurobondInstrumentDto;
import com.company.finance_api.repository.EurobondHistoryRepository;
import com.company.finance_api.repository.EurobondInstrumentRepository;
import com.company.finance_api.repository.EurobondQuoteRepository;
import java.math.BigDecimal;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TrEurobondMarketServiceTest {

  @Mock private EurobondInstrumentRepository instrumentRepository;
  @Mock private EurobondQuoteRepository quoteRepository;
  @Mock private EurobondHistoryRepository historyRepository;

  @InjectMocks private TrEurobondMarketService service;

  @Test
  void listActiveInstruments_loadsLatestQuotesInBulk() {
    EurobondInstrument first =
        instrument("US900123AL40", "Turkey 2030", LocalDate.of(2030, 1, 15), "11.8750");
    EurobondInstrument second =
        instrument("US900123AT75", "Turkey 2034", LocalDate.of(2034, 2, 14), "8.0000");
    EurobondQuote firstQuote =
        quote(
            "US900123AL40",
            "102.125000",
            "6.120000",
            "0.080000",
            Instant.parse("2026-05-25T18:30:00Z"));
    EurobondQuote secondQuote =
        quote(
            "US900123AT75",
            "98.640000",
            "6.450000",
            "-0.050000",
            Instant.parse("2026-05-25T18:31:00Z"));

    when(instrumentRepository.findAllByActiveIsTrueOrderByMaturityDateAsc())
        .thenReturn(List.of(first, second));
    when(quoteRepository.findLatestByIsinIn(List.of("US900123AL40", "US900123AT75")))
        .thenReturn(List.of(firstQuote, secondQuote));

    List<EurobondInstrumentDto> result = service.listActiveInstruments();

    assertEquals(2, result.size());
    assertEquals("US900123AL40", result.get(0).isin());
    assertEquals(new BigDecimal("102.125000"), result.get(0).cleanPrice());
    assertEquals("US900123AT75", result.get(1).isin());
    assertEquals(new BigDecimal("98.640000"), result.get(1).cleanPrice());
    assertNotNull(result.get(0).remainingYears());
    verify(quoteRepository).findLatestByIsinIn(List.of("US900123AL40", "US900123AT75"));
    verify(quoteRepository, never()).findFirstByIsinOrderByQuoteTimeDesc("US900123AL40");
    verify(quoteRepository, never()).findFirstByIsinOrderByQuoteTimeDesc("US900123AT75");
    verifyNoInteractions(historyRepository);
  }

  private static EurobondInstrument instrument(
      String isin, String name, LocalDate maturityDate, String couponPercent) {
    EurobondInstrument instrument = instantiate(EurobondInstrument.class);
    ReflectionTestUtils.setField(instrument, "isin", isin);
    ReflectionTestUtils.setField(instrument, "symbol", isin);
    ReflectionTestUtils.setField(instrument, "name", name);
    ReflectionTestUtils.setField(instrument, "issuer", "Republic of Turkey");
    ReflectionTestUtils.setField(instrument, "currency", "USD");
    ReflectionTestUtils.setField(instrument, "maturityDate", maturityDate);
    ReflectionTestUtils.setField(instrument, "couponPercent", new BigDecimal(couponPercent));
    ReflectionTestUtils.setField(instrument, "couponFrequency", "SEMI_ANNUAL");
    ReflectionTestUtils.setField(instrument, "sourceProvider", "TEST");
    ReflectionTestUtils.setField(instrument, "active", true);
    return instrument;
  }

  private static EurobondQuote quote(
      String isin,
      String cleanPrice,
      String yieldToMaturityPercent,
      String dailyChangePercent,
      Instant quoteTime) {
    EurobondQuote quote = instantiate(EurobondQuote.class);
    ReflectionTestUtils.setField(quote, "isin", isin);
    ReflectionTestUtils.setField(quote, "cleanPrice", new BigDecimal(cleanPrice));
    ReflectionTestUtils.setField(quote, "yieldToMaturityPercent", new BigDecimal(yieldToMaturityPercent));
    ReflectionTestUtils.setField(quote, "dailyChangePercent", new BigDecimal(dailyChangePercent));
    ReflectionTestUtils.setField(quote, "quoteTime", quoteTime);
    ReflectionTestUtils.setField(quote, "sourceProvider", "TEST");
    ReflectionTestUtils.setField(quote, "createdAt", quoteTime);
    return quote;
  }

  private static <T> T instantiate(Class<T> type) {
    try {
      Constructor<T> constructor = type.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), ex);
    }
  }
}
