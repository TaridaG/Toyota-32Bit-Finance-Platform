package com.company.marketdataservice.rates.infrastructure.http;

import com.company.marketdataservice.rates.application.BondYieldHistoryService;
import com.company.marketdataservice.rates.application.CpiHistoryService;
import com.company.marketdataservice.rates.application.PolicyRateHistoryService;
import com.company.marketdataservice.rates.application.RepoRateHistoryService;
import com.company.marketdataservice.rates.application.TlDepositIndexService;
import com.company.marketdataservice.rates.application.TlDepositHistoryService;
import com.company.marketdataservice.rates.infrastructure.http.dto.BankRatesResponseDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRateLatestDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.TlDepositIndexLatestDto;
import com.company.marketdataservice.rates.infrastructure.provider.bank.BankRatesAsset;
import com.company.marketdataservice.rates.infrastructure.provider.bank.DovizBankRatesService;
import com.company.marketdataservice.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatesControllerTest {

    @Mock
    private PolicyRateHistoryService policyRateHistoryService;
    @Mock
    private RepoRateHistoryService repoRateHistoryService;
    @Mock
    private TlDepositHistoryService tlDepositHistoryService;
    @Mock
    private TlDepositIndexService tlDepositIndexService;
    @Mock
    private CpiHistoryService cpiHistoryService;
    @Mock
    private DovizBankRatesService dovizBankRatesService;
    @Mock
    private BondYieldHistoryService bondYieldHistoryService;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        RatesController controller = new RatesController(
                policyRateHistoryService,
                repoRateHistoryService,
                tlDepositHistoryService,
                tlDepositIndexService,
                cpiHistoryService,
                dovizBankRatesService,
                bondYieldHistoryService
        );
        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void policyRateLatest_returnsLatestDto() {
        PolicyRateLatestDto dto = new PolicyRateLatestDto();
        dto.setValue(new BigDecimal("45.00"));
        dto.setDecisionDate(LocalDate.of(2026, 5, 1));
        when(policyRateHistoryService.loadLatest()).thenReturn(dto);

        webTestClient.get()
                .uri("/api/v1/rates/policy-rate/latest")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.value").isEqualTo(45.0)
                .jsonPath("$.decisionDate").isEqualTo("2026-05-01");
    }

    @Test
    void policyRateHistory_rejectsUnsupportedRange() {
        webTestClient.get()
                .uri("/api/v1/rates/policy-rate/history?range=1Y")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    void tlDepositLatest_mapsValidationErrorToBadRequest() {
        when(tlDepositHistoryService.loadLatest("bad"))
                .thenThrow(new IllegalArgumentException("Unsupported maturity: bad"));

        webTestClient.get()
                .uri("/api/v1/rates/tl-deposit/latest?maturity=bad")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.message").isEqualTo("Unsupported maturity: bad");
    }

    @Test
    void tlDepositIndexLatest_returnsIndexPayload() {
        TlDepositIndexLatestDto dto = new TlDepositIndexLatestDto();
        dto.setMaturityCode("MT04");
        dto.setAsOfDate(LocalDate.of(2026, 5, 20));
        dto.setIndexValue(new BigDecimal("1.1234567890"));
        when(tlDepositIndexService.loadLatest("MT04", null)).thenReturn(dto);

        webTestClient.get()
                .uri("/api/v1/rates/tl-deposit/index/latest?maturity=MT04")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.maturityCode").isEqualTo("MT04")
                .jsonPath("$.asOfDate").isEqualTo("2026-05-20")
                .jsonPath("$.indexValue").isEqualTo(1.1234567890);
    }

    @Test
    void bankRates_returnsProviderPayload() {
        BankRatesResponseDto response = new BankRatesResponseDto();
        response.setAsset("USD");
        response.setTitle("Bank rates");
        when(dovizBankRatesService.load(BankRatesAsset.USD)).thenReturn(Mono.just(response));

        webTestClient.get()
                .uri("/api/v1/rates/bank-rates?asset=USD")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.asset").isEqualTo("USD");
    }

    @Test
    void bankRates_rejectsUnsupportedAsset() {
        webTestClient.get()
                .uri("/api/v1/rates/bank-rates?asset=CHF")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    void bankRates_mapsProviderFailureToBadGatewayWithoutInternalMessage() {
        when(dovizBankRatesService.load(BankRatesAsset.USD))
                .thenReturn(Mono.error(new IllegalStateException("upstream timeout details")));

        webTestClient.get()
                .uri("/api/v1/rates/bank-rates?asset=USD")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
                .expectBody()
                .jsonPath("$.error.message").isEqualTo("Bank rates provider unavailable");
    }
}
