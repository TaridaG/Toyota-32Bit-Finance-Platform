package com.company.finance_api.service.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.portfolio.TlDepositInstruments;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class TlDepositIndexQueryService {

  private final JdbcTemplate jdbcTemplate;
  private final RestClient restClient = RestClient.create();

  @Value("${clients.market-data.base-url:http://market-data-service:8080}")
  private String marketDataBaseUrl;

  public TlDepositIndexQueryService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public boolean supports(Instrument instrument) {
    return TlDepositInstruments.isTlDeposit(instrument);
  }

  public Optional<InstrumentPrice> getLatestPrice(Instrument instrument) {
    return lookupPrice(instrument, null, false);
  }

  public Optional<InstrumentPrice> getLatestPriceBefore(Instrument instrument, Instant exclusiveEnd) {
    return lookupPrice(instrument, exclusiveEnd, true);
  }

  public Optional<Instant> getFirstAvailableInstant(Instrument instrument) {
    Optional<Instant> found = queryBoundaryInstant(instrument, true);
    if (found.isEmpty()) {
      warmup(instrument);
      return queryBoundaryInstant(instrument, true);
    }
    return found;
  }

  public Optional<Instant> getLastAvailableInstant(Instrument instrument) {
    Optional<Instant> found = queryBoundaryInstant(instrument, false);
    if (found.isEmpty()) {
      warmup(instrument);
      return queryBoundaryInstant(instrument, false);
    }
    return found;
  }

  private Optional<InstrumentPrice> lookupPrice(
      Instrument instrument, Instant exclusiveEnd, boolean useExclusiveEnd) {
    String maturity = maturityOf(instrument);
    Optional<PriceRow> row = queryPriceRow(maturity, exclusiveEnd, useExclusiveEnd);
    if (row.isEmpty()) {
      warmup(instrument);
      row = queryPriceRow(maturity, exclusiveEnd, useExclusiveEnd);
    }
    return row.map(
        found ->
            new InstrumentPrice(
                instrument,
                PriceType.MARKET,
                found.price(),
                found.day().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusMillis(1)));
  }

  private Optional<PriceRow> queryPriceRow(
      String maturity, Instant exclusiveEnd, boolean useExclusiveEnd) {
    if (!useExclusiveEnd) {
      String sql =
          """
          SELECT day, index_value
          FROM public.mds_evds_tl_deposit_daily_index
          WHERE maturity_code = ?
          ORDER BY day DESC, id DESC
          LIMIT 1
          """;
      return jdbcTemplate.query(
          sql,
          rs ->
              rs.next()
                  ? Optional.of(new PriceRow(rs.getDate(1).toLocalDate(), rs.getBigDecimal(2)))
                  : Optional.empty(),
          maturity);
    }
    Instant target = exclusiveEnd != null ? exclusiveEnd.minusMillis(1) : Instant.now();
    LocalDate targetDay = target.atZone(ZoneOffset.UTC).toLocalDate();
    String sql =
        """
        SELECT day, index_value
        FROM public.mds_evds_tl_deposit_daily_index
        WHERE maturity_code = ?
          AND day <= ?
        ORDER BY day DESC, id DESC
        LIMIT 1
        """;
    return jdbcTemplate.query(
        sql,
        rs ->
            rs.next()
                ? Optional.of(new PriceRow(rs.getDate(1).toLocalDate(), rs.getBigDecimal(2)))
                : Optional.empty(),
        maturity,
        Date.valueOf(targetDay));
  }

  private Optional<Instant> queryBoundaryInstant(Instrument instrument, boolean first) {
    String maturity = maturityOf(instrument);
    String sql =
        first
            ? """
              SELECT day
              FROM public.mds_evds_tl_deposit_daily_index
              WHERE maturity_code = ?
              ORDER BY day ASC, id ASC
              LIMIT 1
              """
            : """
              SELECT day
              FROM public.mds_evds_tl_deposit_daily_index
              WHERE maturity_code = ?
              ORDER BY day DESC, id DESC
              LIMIT 1
              """;
    return jdbcTemplate.query(
        sql,
        rs ->
            rs.next()
                ? Optional.of(rs.getDate(1).toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC))
                : Optional.empty(),
        maturity);
  }

  private void warmup(Instrument instrument) {
    String maturity = maturityOf(instrument);
    String base = marketDataBaseUrl.replaceAll("/+$", "");
    String uri =
        UriComponentsBuilder.fromUriString(base)
            .path("/api/rates/tl-deposit/index/latest")
            .queryParam("maturity", maturity)
            .build(true)
            .toUriString();
    try {
      restClient.get().uri(uri).retrieve().toBodilessEntity();
    } catch (Exception ignored) {
      // Best-effort warmup only; callers handle empty data after retry.
    }
  }

  private String maturityOf(Instrument instrument) {
    return TlDepositInstruments.resolveMaturityCode(instrument)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unsupported TL deposit instrument: "
                        + (instrument != null ? instrument.getSymbol() : "null")));
  }

  private record PriceRow(LocalDate day, BigDecimal price) {}
}
