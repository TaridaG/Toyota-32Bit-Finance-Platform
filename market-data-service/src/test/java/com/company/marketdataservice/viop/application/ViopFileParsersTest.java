package com.company.marketdataservice.viop.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.marketdataservice.viop.infrastructure.source.BistDerivativesFileClient.DownloadedFile;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
    void parsesSettlementsFromCsv() {
        String csv = "Tarih;SozlesmeKodu;SonFiyat;Degisim;HacimTL;AcikPozisyon\n"
                + "31.05.2026;F_TLREF0626;43,25;1,20;12.500.000,00;5.432,00\n";
        DownloadedFile file = new DownloadedFile("x", "settlement.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        var rows = ViopFileParsers.parseSettlements(file, LocalDate.of(2026, 5, 31));
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().contractCode()).isEqualTo("F_TLREF0626");
        assertThat(rows.getFirst().lastPrice()).hasToString("43.25");
    }
}

