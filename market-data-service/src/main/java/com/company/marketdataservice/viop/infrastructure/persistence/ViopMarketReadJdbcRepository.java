package com.company.marketdataservice.viop.infrastructure.persistence;

import com.company.marketdataservice.viop.domain.ViopContractSegment;
import com.company.marketdataservice.viop.infrastructure.http.dto.ViopActiveContractDto;
import com.company.marketdataservice.viop.infrastructure.http.dto.ViopSettlementHistoryPointDto;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * VIOP aktif sözleşme ve settlement geçmişi sorguları için JDBC tabanlı persistence erişimi.
 */
@Repository
@RequiredArgsConstructor
public class ViopMarketReadJdbcRepository {

    private static final int ALL_SEGMENT_LIMIT = 300;

    private final JdbcTemplate jdbcTemplate;

    public List<ViopActiveContractDto> findActiveContracts(ViopContractSegment segment) {
        StringBuilder sql =
                new StringBuilder(
                        """
                SELECT c.contract_code,
                       c.underlying,
                       c.market_group,
                       c.expiry_date,
                       s.trade_date,
                       s.last_price,
                       s.change_percent,
                       s.volume_tl,
                       s.volume_qty,
                       s.open_interest,
                       s.ingested_at
                FROM mds_viop_contract_catalog c
                JOIN mds_viop_daily_settlement s
                  ON s.contract_code = c.contract_code
                JOIN (
                    SELECT contract_code, MAX(trade_date) AS trade_date
                    FROM mds_viop_daily_settlement
                    GROUP BY contract_code
                ) latest
                  ON latest.contract_code = s.contract_code
                 AND latest.trade_date = s.trade_date
                WHERE c.is_active = TRUE
                """);

        List<Object> params = new ArrayList<>();
        if (segment.filtersByPazar()) {
            String placeholders =
                    segment.pazarCodes().stream().map(code -> "?").collect(Collectors.joining(", "));
            sql.append("  AND UPPER(c.pazar) IN (").append(placeholders).append(")\n");
            for (String code : segment.pazarCodes()) {
                params.add(code.toUpperCase(Locale.ROOT));
            }
        }

        if (segment == ViopContractSegment.ALL) {
            sql.append("ORDER BY s.volume_tl DESC NULLS LAST, c.expiry_date ASC NULLS LAST, c.contract_code ASC\n");
            sql.append("LIMIT ").append(ALL_SEGMENT_LIMIT);
        } else {
            sql.append("ORDER BY c.expiry_date ASC NULLS LAST, c.contract_code ASC");
        }

        return jdbcTemplate.query(
                sql.toString(),
                ps -> {
                    for (int i = 0; i < params.size(); i++) {
                        ps.setObject(i + 1, params.get(i));
                    }
                },
                (rs, rowNum) ->
                        new ViopActiveContractDto(
                                rs.getString("contract_code"),
                                rs.getString("underlying"),
                                rs.getString("market_group"),
                                rs.getObject("expiry_date", java.time.LocalDate.class),
                                rs.getObject("trade_date", java.time.LocalDate.class),
                                rs.getBigDecimal("last_price"),
                                rs.getBigDecimal("change_percent"),
                                rs.getBigDecimal("volume_tl"),
                                rs.getBigDecimal("volume_qty"),
                                rs.getBigDecimal("open_interest"),
                                rs.getTimestamp("ingested_at") == null
                                        ? null
                                        : rs.getTimestamp("ingested_at").toInstant()));
    }

    public List<ViopSettlementHistoryPointDto> findHistory(
            String contractCode, java.time.LocalDate from, java.time.LocalDate to) {
        return jdbcTemplate.query(
                """
                SELECT trade_date, last_price
                FROM mds_viop_daily_settlement
                WHERE contract_code = ?
                  AND trade_date >= ?
                  AND trade_date <= ?
                ORDER BY trade_date ASC
                """,
                ps -> {
                    ps.setString(1, contractCode);
                    ps.setObject(2, Date.valueOf(from));
                    ps.setObject(3, Date.valueOf(to));
                },
                (rs, rowNum) ->
                        new ViopSettlementHistoryPointDto(
                                rs.getObject("trade_date", java.time.LocalDate.class),
                                rs.getBigDecimal("last_price")));
    }
}
