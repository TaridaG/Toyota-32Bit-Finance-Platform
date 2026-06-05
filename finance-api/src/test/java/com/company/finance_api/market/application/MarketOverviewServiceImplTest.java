package com.company.finance_api.market.application;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.company.finance_api.instrument.application.InstrumentService;
import com.company.finance_api.instrument.infrastructure.persistence.InstrumentRepository;
import com.company.finance_api.pricing.application.CurrencyConversionService;
import com.company.finance_api.pricing.infrastructure.persistence.InstrumentPriceRepository;
import com.company.finance_api.shared.cache.JsonCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MarketOverviewServiceImplTest {

  @Mock private InstrumentService instrumentService;
  @Mock private InstrumentRepository instrumentRepository;
  @Mock private InstrumentPriceRepository instrumentPriceRepository;
  @Mock private CurrencyConversionService currencyConversionService;
  @Mock private JsonCacheService jsonCacheService;

  @Test
  void loadMergedBaseItems_allCategory_mergesFxRatesForMissingPrices() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/api/v1/market/prices",
        exchange -> {
          byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
          }
        });
    server.createContext(
        "/api/v1/market/fx",
        exchange -> {
          byte[] body =
              "[{\"symbol\":\"EURTRY\",\"mid\":53.46,\"source\":\"TCMB\"}]"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
          }
        });
    server.start();
    int port = server.getAddress().getPort();

    try {
      when(instrumentService.getAllActive()).thenReturn(List.of());
      when(instrumentRepository.findByActiveFalse()).thenReturn(List.of());

      MarketOverviewServiceImpl service =
          new MarketOverviewServiceImpl(
              instrumentService,
              instrumentRepository,
              instrumentPriceRepository,
              currencyConversionService,
              new ObjectMapper(),
              jsonCacheService,
              10);
      ReflectionTestUtils.setField(
          service, "marketDataBaseUrl", "http://127.0.0.1:" + port);

      @SuppressWarnings("unchecked")
      List<Object> items =
          (List<Object>)
              ReflectionTestUtils.invokeMethod(service, "loadMergedBaseItems", null, "ALL");

      assertTrue(
          items.stream()
              .anyMatch(
                  item -> {
                    try {
                      String symbol = (String) item.getClass().getMethod("symbol").invoke(item);
                      if (!"EURTRY".equals(symbol)) {
                        return false;
                      }
                      BigDecimal price =
                          (BigDecimal) item.getClass().getMethod("price").invoke(item);
                      return price != null && price.compareTo(BigDecimal.ZERO) > 0;
                    } catch (ReflectiveOperationException ex) {
                      throw new RuntimeException(ex);
                    }
                  }),
          "EURTRY should be present with FX mid price on ALL category load");
    } finally {
      server.stop(0);
    }
  }
}
