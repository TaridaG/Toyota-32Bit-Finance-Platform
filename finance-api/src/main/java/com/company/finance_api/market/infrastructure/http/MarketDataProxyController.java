package com.company.finance_api.market.infrastructure.http;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Forwards browser / SPA traffic for market-data-service routes through finance-api so local Vite
 * only needs {@code VITE_PROXY_TARGET} (:8080). API gateway sends {@code
 * /api/market/instruments/.../fundamentals} straight to market-data-service; finance still exposes
 * {@link MarketFundamentalsPassthroughController} for direct finance calls. {@code
 * /api/market/overview} stays on {@link MarketOverviewController}.
 * Policy rate history ({@code /api/rates/**}) is proxied the same way as market prices.
 */
/** market-data-service'e HTTP proxy; portal isteklerini upstream'e iletir. */
@RestController
public class MarketDataProxyController {

  private static final Logger log = LoggerFactory.getLogger(MarketDataProxyController.class);

  private final RestClient restClient = RestClient.create();

  @Value("${clients.market-data.base-url:http://market-data-service:8080}")
  private String marketDataBaseUrl;

  @RequestMapping(
      method = {RequestMethod.GET, RequestMethod.HEAD},
      value = {
        "/api/market/prices",
        "/api/market/prices/**",
        "/api/market/segments/**",
        "/api/market/fx",
        "/api/market/fx/**",
        "/api/market/funds",
        "/api/market/funds/**",
        "/api/market/ingestion/**",
        "/api/market/debug/**",
        "/api/rates",
        "/api/rates/**"
      })
  public ResponseEntity<byte[]> proxy(HttpServletRequest request) {
    String base = marketDataBaseUrl.replaceAll("/+$", "");
    URI downstream =
        UriComponentsBuilder.fromUriString(base)
            .path(request.getRequestURI())
            .query(request.getQueryString())
            .build(true)
            .toUri();

    HttpMethod method = HttpMethod.valueOf(request.getMethod());
    try {
      return restClient
          .method(method)
          .uri(downstream)
          .headers(h -> copySelectRequestHeaders(request, h))
          .retrieve()
          .toEntity(byte[].class);
    } catch (ResourceAccessException | RestClientResponseException ex) {
      log.warn("Market data proxy failed uri={} reason={}", downstream, ex.toString());
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }
  }

  private static void copySelectRequestHeaders(HttpServletRequest request, HttpHeaders out) {
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
