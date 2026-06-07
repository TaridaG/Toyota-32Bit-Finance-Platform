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
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * market-data-service'e HTTP proxy; SPA/browser trafiğini finance-api üzerinden upstream'e iletir
 * (local Vite yalnızca {@code VITE_PROXY_TARGET} :8080 kullanır). {@code /api/v1/market/overview}
 * {@link MarketOverviewController} üzerinde kalır; policy rate history ({@code /api/v1/rates/**})
 * market price route'ları ile aynı şekilde proxy edilir.
 */
@RestController
public class MarketDataProxyController {

  private static final Logger log = LoggerFactory.getLogger(MarketDataProxyController.class);

  private final RestClient restClient = RestClient.create();

  @Value("${clients.market-data.base-url:http://market-data-service:8080}")
  private String marketDataBaseUrl;

  @RequestMapping(
      method = {RequestMethod.GET, RequestMethod.HEAD, RequestMethod.POST},
      value = {
        "/api/v1/market/prices",
        "/api/v1/market/prices/**",
        "/api/v1/market/segments/**",
        "/api/v1/market/fx",
        "/api/v1/market/fx/**",
        "/api/v1/market/funds",
        "/api/v1/market/funds/**",
        "/api/v1/market/viop/**",
        "/api/v1/market/ingest/**",
        "/api/v1/market/ingestion/**",
        "/api/v1/market/debug/**",
        "/api/v1/rates",
        "/api/v1/rates/**"
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
      byte[] requestBody = StreamUtils.copyToByteArray(request.getInputStream());
      RestClient.RequestBodySpec spec =
          restClient
          .method(method)
          .uri(downstream)
          .headers(h -> copySelectRequestHeaders(request, h));
      if (hasRequestBody(method) && requestBody.length > 0) {
        spec = spec.body(requestBody);
      }
      return spec.retrieve().toEntity(byte[].class);
    } catch (ResourceAccessException | RestClientResponseException ex) {
      log.warn("Market data proxy failed uri={} reason={}", downstream, ex.toString());
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    } catch (Exception ex) {
      log.warn("Market data proxy request read failed uri={} reason={}", downstream, ex.toString());
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }
  }

  private static boolean hasRequestBody(HttpMethod method) {
    return HttpMethod.POST.equals(method)
        || HttpMethod.PUT.equals(method)
        || HttpMethod.PATCH.equals(method);
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
