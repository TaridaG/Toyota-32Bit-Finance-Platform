package com.company.finance_api.market.infrastructure.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Dedicated mapping so {@code GET /api/v1/market/instruments/{symbol}/fundamentals} always resolves.
 * TRY FX crosses have no equity-style fundamentals; return a stable payload without calling MDS.
 */
@RestController
public class MarketFundamentalsPassthroughController {

  private final ObjectMapper objectMapper;
  private final RestClient marketMdsClient = RestClient.create();

  @Value("${clients.market-data.base-url:http://market-data-service:8080}")
  private String marketDataBaseUrl;

  public MarketFundamentalsPassthroughController(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** HTTP handler — `/api/v1/market/instruments/{symbol}/fundamentals` endpoint'i. */
  @GetMapping("/api/v1/market/instruments/{symbol}/fundamentals")
  public ResponseEntity<byte[]> fundamentals(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "false") boolean forceRefresh,
      HttpServletRequest request) {
    String normalized = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
    if (isTryFxCross(normalized)) {
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(syntheticTryFxJson(normalized));
    }

    String base = marketDataBaseUrl.replaceAll("/+$", "");
    URI uri =
        UriComponentsBuilder.fromUriString(base)
            .path("/api/v1/market/instruments/{s}/fundamentals")
            .queryParam("forceRefresh", forceRefresh)
            .buildAndExpand(normalized)
            .toUri();
    try {
      return marketMdsClient
          .get()
          .uri(uri)
          .headers(h -> copyMdsProxyHeaders(request, h))
          .retrieve()
          .toEntity(byte[].class);
    } catch (RestClientResponseException ex) {
      byte[] body = ex.getResponseBodyAsByteArray();
      if (body == null || body.length == 0) {
        return ResponseEntity.status(ex.getStatusCode()).build();
      }
      return ResponseEntity.status(ex.getStatusCode())
          .contentType(MediaType.APPLICATION_JSON)
          .body(body);
    } catch (ResourceAccessException ex) {
      return ResponseEntity.status(502).build();
    }
  }

  private static boolean isTryFxCross(String s) {
    if (!StringUtils.hasText(s) || !s.endsWith("TRY") || s.length() <= 3) {
      return false;
    }
    String base = s.substring(0, s.length() - 3);
    if (!base.matches("[A-Z0-9]+")) {
      return false;
    }
    // TCMB-style metals (XAU, XAG, …) — keep MDS path like other instruments
    if (base.length() == 3 && base.matches("X[A-Z]{2}")) {
      return false;
    }
    return true;
  }

  private byte[] syntheticTryFxJson(String sym) {
    try {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("symbol", sym);
      m.put("provider", "INTERNAL_META");
      m.put("providerSymbol", sym);
      m.put("companyName", sym + " Exchange Rate");
      m.put("country", null);
      m.put("currency", "TRY");
      m.put("exchange", null);
      m.put("ipoDate", null);
      m.put("industry", "Foreign Exchange");
      m.put("website", null);
      m.put("marketCapitalization", null);
      m.put("sharesOutstanding", null);
      m.put("peTtm", null);
      m.put("epsTtm", null);
      m.put("fetchedAt", Instant.now().toString());
      m.put("cacheHit", false);
      m.put("annualStatements", List.of());
      return objectMapper.writeValueAsBytes(m);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void copyMdsProxyHeaders(HttpServletRequest request, HttpHeaders out) {
    String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (StringUtils.hasText(auth)) {
      out.set(HttpHeaders.AUTHORIZATION, auth);
    }
    String currency = request.getHeader("X-Currency");
    if (StringUtils.hasText(currency)) {
      out.set("X-Currency", currency);
    }
    String accept = request.getHeader(HttpHeaders.ACCEPT);
    if (StringUtils.hasText(accept)) {
      out.set(HttpHeaders.ACCEPT, accept);
    }
    String acceptLanguage = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
    if (StringUtils.hasText(acceptLanguage)) {
      out.set(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage);
    }
  }
}
