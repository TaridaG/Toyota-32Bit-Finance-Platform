package com.company.marketdataservice.viop.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.marketdataservice.bootstrap.config.ViopMarketProperties;
import com.company.marketdataservice.viop.domain.ViopContractRow;
import com.company.marketdataservice.viop.domain.ViopSettlementRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ViopAliasResolverTest {

    @Test
    void picksNearestContractPerAlias() {
        ViopAliasResolver resolver = new ViopAliasResolver(new ViopMarketProperties());
        LocalDate tradeDate = LocalDate.of(2026, 6, 1);

        List<ViopContractRow> contracts = List.of(
                new ViopContractRow("F_TLREF0626", "TLREF", "FUTURE", "FAIZ", LocalDate.of(2026, 6, 30), null, "TRY", "x"),
                new ViopContractRow("F_TLREF0726", "TLREF", "FUTURE", "FAIZ", LocalDate.of(2026, 7, 31), null, "TRY", "x"),
                new ViopContractRow("F_DIBS0626", "DIBS", "FUTURE", "TAHVIL", LocalDate.of(2026, 6, 30), null, "TRY", "x"));
        List<ViopSettlementRow> rows = List.of(
                new ViopSettlementRow(tradeDate, "F_TLREF0626", new BigDecimal("42.10"), null, null, null, null, null, "x"),
                new ViopSettlementRow(tradeDate, "F_TLREF0726", new BigDecimal("43.10"), null, null, null, null, null, "x"),
                new ViopSettlementRow(tradeDate, "F_DIBS0626", new BigDecimal("38.55"), null, null, null, null, null, "x"));

        var aliases = resolver.resolve(contracts, rows, tradeDate);
        assertThat(aliases).extracting(a -> a.aliasSymbol()).contains("VIOP_TLREF_NEAR", "VIOP_DIBS_NEAR");
        assertThat(aliases).anySatisfy(a -> {
            if ("VIOP_TLREF_NEAR".equals(a.aliasSymbol())) {
                assertThat(a.contractCode()).isEqualTo("F_TLREF0626");
            }
        });
    }
}

