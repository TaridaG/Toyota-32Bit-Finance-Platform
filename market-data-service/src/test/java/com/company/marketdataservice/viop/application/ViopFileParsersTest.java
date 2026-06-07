package com.company.marketdataservice.viop.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.marketdataservice.viop.domain.ViopSettlementRow;
import com.company.marketdataservice.viop.infrastructure.source.BistDerivativesFileClient.DownloadedFile;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ViopFileParsersTest {

    @Test
    void parsesContractsFromCsv() {
        String csv = "SozlesmeKodu;DayanakVarlik;VadeTarihi;ParaBirimi\n"
                + "F_TLREF0626;TLREF;28.06.2026;TRY\n";
        DownloadedFile file = new DownloadedFile("x", "contracts.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        var rows = ViopFileParsers.parseContracts(file);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().contractCode()).isEqualTo("F_TLREF0626");
        assertThat(rows.getFirst().expiryDate()).isEqualTo(LocalDate.of(2026, 6, 28));
    }

    @Test
    void parsesSettlementsFromLegacyTurkishCsv() {
        String csv = "Tarih;SozlesmeKodu;SonFiyat;Degisim;HacimTL;AcikPozisyon\n"
                + "31.05.2026;F_TLREF0626;43,25;1,20;12.500.000,00;5.432,00\n";
        DownloadedFile file = new DownloadedFile("x", "settlement.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        var rows = ViopFileParsers.parseSettlements(file, LocalDate.of(2026, 5, 31));
        assertThat(rows).hasSize(1);
        ViopSettlementRow row = rows.getFirst();
        assertThat(row.contractCode()).isEqualTo("F_TLREF0626");
        assertThat(row.lastPrice()).hasToString("43.25");
        assertThat(row.changePercent()).isEqualByComparingTo(new BigDecimal("1.20"));
        assertThat(row.volumeTl()).hasToString("12500000.00");
        assertThat(row.openInterest()).hasToString("5432.00");
    }

    @Test
    void parsesSettlementsFromOfficialBistCsvFormat() {
        String csv =
                "TARIH;SOZLESME KODU;UZLASMA FIYATI;UZLASMA FIYATI DEGISIMI (%);ISLEM HACMI;ISLEM MIKTARI;ACIK POZISYON\n"
                        + "TRADE DATE;INSTRUMENT SERIES;SETTLEMENT PRICE;CHANGE OF SETTLEMENT PRICE (%);TRADED VALUE;TRADE VOLUME;OPEN POSITION\n"
                        + "2026-06-02;F_TLREF1M0626;39.41;0;0;0;0\n"
                        + "2026-06-02;F_AKBNK0626;53.92;2.06;2326967602;434560;612860\n";
        DownloadedFile file =
                new DownloadedFile("x", "viop_20260602.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        List<ViopSettlementRow> rows = ViopFileParsers.parseSettlements(file, LocalDate.of(2026, 6, 2));

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(ViopSettlementRow::contractCode).doesNotContain("INSTRUMENT SERIES");

        ViopSettlementRow tlref = rows.stream()
                .filter(r -> "F_TLREF1M0626".equals(r.contractCode()))
                .findFirst()
                .orElseThrow();
        assertThat(tlref.lastPrice()).isEqualByComparingTo(new BigDecimal("39.41"));
        assertThat(tlref.changePercent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(tlref.volumeTl()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(tlref.openInterest()).isEqualByComparingTo(BigDecimal.ZERO);

        ViopSettlementRow akbnk = rows.stream()
                .filter(r -> "F_AKBNK0626".equals(r.contractCode()))
                .findFirst()
                .orElseThrow();
        assertThat(akbnk.lastPrice()).isEqualByComparingTo(new BigDecimal("53.92"));
        assertThat(akbnk.changePercent()).isEqualByComparingTo(new BigDecimal("2.06"));
        assertThat(akbnk.volumeTl()).isEqualByComparingTo(new BigDecimal("2326967602"));
        assertThat(akbnk.volumeQty()).isEqualByComparingTo(new BigDecimal("434560"));
        assertThat(akbnk.openInterest()).isEqualByComparingTo(new BigDecimal("612860"));
    }
}
