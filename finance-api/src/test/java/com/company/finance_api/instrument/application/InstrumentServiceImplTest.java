package com.company.finance_api.instrument.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.instrument.domain.enums.Exchange;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import com.company.finance_api.instrument.infrastructure.persistence.InstrumentRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstrumentServiceImplTest {

  @Mock private InstrumentRepository instrumentRepository;

  @InjectMocks private InstrumentServiceImpl instrumentService;

  @Test
  void getAllActive_delegatesToRepository() {
    Instrument btc =
        new Instrument("BTCUSDT", "Bitcoin", InstrumentType.CRYPTO, Exchange.BINANCE);
    when(instrumentRepository.findByActiveTrue()).thenReturn(List.of(btc));

    assertEquals(1, instrumentService.getAllActive().size());
    assertEquals("BTCUSDT", instrumentService.getAllActive().get(0).getSymbol());
  }

  @Test
  void getByType_delegatesToRepository() {
    Instrument stock = new Instrument("ASELS", "Aselsan", InstrumentType.STOCK, Exchange.BIST);
    when(instrumentRepository.findByTypeAndActiveTrue(InstrumentType.STOCK))
        .thenReturn(List.of(stock));

    var result = instrumentService.getByType(InstrumentType.STOCK);

    assertEquals(1, result.size());
    assertEquals(InstrumentType.STOCK, result.get(0).getType());
  }
}
