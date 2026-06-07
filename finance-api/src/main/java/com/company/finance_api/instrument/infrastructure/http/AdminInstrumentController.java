package com.company.finance_api.instrument.infrastructure.http;

import com.company.finance_api.instrument.application.InstrumentService;
import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.instrument.domain.enums.Exchange;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import com.company.finance_api.instrument.infrastructure.http.dto.AdminCreateInstrumentRequest;
import com.company.finance_api.shared.web.ApiResponse;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/** Admin enstrüman oluşturma ve pasifleştirme REST endpoint'leri. */
@RestController
@RequestMapping("/api/v1/admin/instruments")
public class AdminInstrumentController {
  private static final Logger log = LoggerFactory.getLogger(AdminInstrumentController.class);
  private static final Set<String> ALLOWED_SEGMENTS = Set.of("CRYPTO", "BIST", "NASDAQ");
  private static final Set<Exchange> NASDAQ_SEGMENT_EXCHANGES =
      Set.of(Exchange.NASDAQ, Exchange.FINNHUB, Exchange.YAHOO);

  private final InstrumentService instrumentService;
  private final RestTemplate restTemplate = new RestTemplate();
  private final String marketDataBaseUrl;

  public AdminInstrumentController(
      InstrumentService instrumentService,
      @Value("${clients.market-data.base-url:http://market-data-service:8080}") String marketDataBaseUrl) {
    this.instrumentService = instrumentService;
    this.marketDataBaseUrl = marketDataBaseUrl;
  }

  @PostMapping
  public ApiResponse<Long> createInstrument(@RequestBody AdminCreateInstrumentRequest request) {
    validateCreateRequest(request);
    String normalizedSymbol = request.symbol().trim().toUpperCase(Locale.ROOT);
    String normalizedName = request.name().trim();
    String normalizedSegment = normalizeSegment(request.segment());
    Instrument instrument =
        instrumentService.createInstrument(
            normalizedSymbol, normalizedName, request.type(), request.exchange());

    if (normalizedSegment != null) {
      // Best-effort call into market-data-service to sync ingest config.
      try {
        String url = marketDataBaseUrl + "/api/v1/market/ingest/config/enable";
        record EnableCommand(Long instrumentId, String segment) {}
        restTemplate.postForObject(
            url, new EnableCommand(instrument.getId(), normalizedSegment), Void.class);
      } catch (Exception ex) {
        log.warn("MDS ingest enable sync failed for instrumentId={} reason={}", instrument.getId(), ex.getMessage());
        // Admin can still enable ingest later from MDS admin UI.
      }
    }

    return ApiResponse.success(instrument.getId());
  }

  @PostMapping("/{instrumentId}/deactivate")
  public ApiResponse<String> deactivateInstrument(@PathVariable Long instrumentId) {
    instrumentService.deactivateInstrument(instrumentId);
    return ApiResponse.success("INSTRUMENT_DELISTED_SAFE_DETACH");
  }

  private static void validateCreateRequest(AdminCreateInstrumentRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Request is required.");
    }
    if (request.symbol() == null || request.symbol().trim().isEmpty()) {
      throw new IllegalArgumentException("symbol is required.");
    }
    if (request.name() == null || request.name().trim().isEmpty()) {
      throw new IllegalArgumentException("name is required.");
    }
    if (request.type() == null) {
      throw new IllegalArgumentException("type is required.");
    }
    if (request.exchange() == null) {
      throw new IllegalArgumentException("exchange is required.");
    }

    String segment = normalizeSegment(request.segment());
    if (segment == null) {
      return;
    }
    if (!ALLOWED_SEGMENTS.contains(segment)) {
      throw new IllegalArgumentException("segment must be one of CRYPTO, BIST, NASDAQ.");
    }

    if ("CRYPTO".equals(segment)) {
      if (request.type() != InstrumentType.CRYPTO) {
        throw new IllegalArgumentException("CRYPTO segment requires type=CRYPTO.");
      }
      if (request.exchange() != Exchange.BINANCE) {
        throw new IllegalArgumentException("CRYPTO segment requires exchange=BINANCE.");
      }
      return;
    }

    if (request.type() != InstrumentType.STOCK) {
      throw new IllegalArgumentException(segment + " segment requires type=STOCK.");
    }
    if ("BIST".equals(segment) && request.exchange() != Exchange.BIST) {
      throw new IllegalArgumentException("BIST segment requires exchange=BIST.");
    }
    if ("NASDAQ".equals(segment) && !NASDAQ_SEGMENT_EXCHANGES.contains(request.exchange())) {
      throw new IllegalArgumentException("NASDAQ segment requires exchange in [NASDAQ, FINNHUB, YAHOO].");
    }
  }

  private static String normalizeSegment(String segmentRaw) {
    if (segmentRaw == null || segmentRaw.trim().isEmpty()) {
      return null;
    }
    return segmentRaw.trim().toUpperCase(Locale.ROOT);
  }
}

