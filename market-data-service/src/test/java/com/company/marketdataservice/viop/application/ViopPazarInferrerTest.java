package com.company.marketdataservice.viop.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.marketdataservice.bootstrap.config.ViopMarketProperties;
import com.company.marketdataservice.viop.domain.ViopContractRow;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ViopPazarInferrerTest {

    private ViopPazarInferrer inferrer;

    @BeforeEach
    void setUp() {
        inferrer = new ViopPazarInferrer(new ViopMarketProperties());
    }

    @Test
    void inferPazar_mapsBondFutureToDBo() {
        assertThat(inferrer.inferPazar(row("F_DIBS0626", "DIBS", "TAHVIL")))
                .isEqualTo("D_BO");
    }

    @Test
    void inferPazar_mapsTlrefFutureToDFi() {
        assertThat(inferrer.inferPazar(row("F_TLREF0626", "TLREF", "TRF")))
                .isEqualTo("D_FI");
    }

    @Test
    void inferPazar_mapsEquityFutureToDEq() {
        assertThat(inferrer.inferPazar(row("F_AKBNK0626", "AKBNK.E", "SSF")))
                .isEqualTo("D_EQ");
    }

    @Test
    void inferPazar_mapsIndexFutureToDIx() {
        assertThat(inferrer.inferPazar(row("F_XU0300826", "XU030", "IDX")))
                .isEqualTo("D_IX");
    }

    @Test
    void inferPazar_mapsFxFutureToDCm() {
        assertThat(inferrer.inferPazar(row("F_USDTRY0726", "USDTRY", "FX")))
                .isEqualTo("D_CM");
    }

    @Test
    void inferMissing_preservesExistingPazar() {
        ViopContractRow withPazar =
                new ViopContractRow(
                        "F_TLREF0626",
                        "TLREF",
                        "FUTURE",
                        "TRF",
                        LocalDate.of(2026, 6, 30),
                        null,
                        "TRY",
                        "D_FI",
                        "x");
        List<ViopContractRow> out = inferrer.inferMissing(List.of(withPazar));
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().pazar()).isEqualTo("D_FI");
    }

    @Test
    void inferMissing_fillsBlankPazarFromOfficialCsvLikeRow() {
        ViopContractRow fromOfficialSettlement =
                new ViopContractRow(
                        "F_TLREF1M0626",
                        "TLREF",
                        "FUTURE",
                        null,
                        LocalDate.of(2026, 6, 30),
                        null,
                        "TRY",
                        null,
                        "viopms.csv");
        List<ViopContractRow> out = inferrer.inferMissing(List.of(fromOfficialSettlement));
        assertThat(out.getFirst().pazar()).isEqualTo("D_FI");
    }

    @Test
    void inferPazar_bondTakesPriorityOverRateKeywordCollision() {
        assertThat(inferrer.inferPazar(row("F_DIBS0626", "DIBS", "FAIZ")))
                .isEqualTo("D_BO");
    }

    private static ViopContractRow row(String code, String underlying, String marketGroup) {
        return new ViopContractRow(
                code, underlying, "FUTURE", marketGroup, LocalDate.of(2026, 6, 30), null, "TRY", null, "test.csv");
    }
}
