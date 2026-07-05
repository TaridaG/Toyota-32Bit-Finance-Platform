package com.company.marketdataservice.viop.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ViopContractSegmentTest {

    @Test
    void resolvesKnownQueryValues() {
        assertThat(ViopContractSegment.fromQuery("rates")).contains(ViopContractSegment.RATES);
        assertThat(ViopContractSegment.fromQuery("equity")).contains(ViopContractSegment.EQUITY);
        assertThat(ViopContractSegment.fromQuery("rates_bonds")).contains(ViopContractSegment.RATES_BONDS);
        assertThat(ViopContractSegment.fromQuery(null)).contains(ViopContractSegment.RATES_BONDS);
    }

    @Test
    void mapsSegmentsToPazarCodes() {
        assertThat(ViopContractSegment.RATES.pazarCodes()).containsExactly("D_FI");
        assertThat(ViopContractSegment.BONDS.pazarCodes()).containsExactly("D_BO");
        assertThat(ViopContractSegment.EQUITY.pazarCodes()).containsExactly("D_EQ");
        assertThat(ViopContractSegment.RATES_BONDS.pazarCodes()).containsExactly("D_FI", "D_BO");
        assertThat(ViopContractSegment.ALL.filtersByPazar()).isFalse();
    }

    @Test
    void rejectsUnknownQueryValue() {
        Optional<ViopContractSegment> resolved = ViopContractSegment.fromQuery("unknown");
        assertThat(resolved).isEmpty();
    }
}
