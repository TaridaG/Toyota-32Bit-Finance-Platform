package com.company.marketdataservice.rates.infrastructure.http;
import com.company.marketdataservice.rates.infrastructure.http.dto.CpiLatestDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRateHistoryResponseDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRateLatestDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.TlDepositIndexHistoryResponseDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.TlDepositIndexLatestDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.TlDepositLatestDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.BankRatesResponseDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.RepoRateLatestDto;
import com.company.marketdataservice.rates.application.CpiHistoryService;
import com.company.marketdataservice.rates.domain.CpiMetric;
import com.company.marketdataservice.rates.application.PolicyRateHistoryService;
import com.company.marketdataservice.rates.application.RepoRateHistoryService;
import com.company.marketdataservice.rates.application.TlDepositIndexService;
import com.company.marketdataservice.rates.application.TlDepositHistoryService;
import com.company.marketdataservice.rates.infrastructure.provider.bank.BankRatesAsset;
import com.company.marketdataservice.rates.infrastructure.provider.bank.DovizBankRatesService;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * `makro oran` REST endpoint'lerini expose eden HTTP controller.
 */
@RestController
@RequestMapping("/api/v1/rates")
public class RatesController {

    private final PolicyRateHistoryService policyRateHistoryService;
    private final RepoRateHistoryService repoRateHistoryService;
    private final TlDepositHistoryService tlDepositHistoryService;
    private final TlDepositIndexService tlDepositIndexService;
    private final CpiHistoryService cpiHistoryService;
    private final DovizBankRatesService dovizBankRatesService;

    public RatesController(
            PolicyRateHistoryService policyRateHistoryService,
            RepoRateHistoryService repoRateHistoryService,
            TlDepositHistoryService tlDepositHistoryService,
            TlDepositIndexService tlDepositIndexService,
            CpiHistoryService cpiHistoryService,
            DovizBankRatesService dovizBankRatesService
    ) {
        this.policyRateHistoryService = policyRateHistoryService;
        this.repoRateHistoryService = repoRateHistoryService;
        this.tlDepositHistoryService = tlDepositHistoryService;
        this.tlDepositIndexService = tlDepositIndexService;
        this.cpiHistoryService = cpiHistoryService;
        this.dovizBankRatesService = dovizBankRatesService;
    }

    @GetMapping("/bank-rates")
    public Mono<BankRatesResponseDto> bankRates(@RequestParam(defaultValue = "USD") String asset) {
        final BankRatesAsset parsed;
        try {
            parsed = BankRatesAsset.parse(asset);
        } catch (IllegalArgumentException ex) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()));
        }
        return dovizBankRatesService
                .load(parsed)
                .onErrorMap(
                        IllegalStateException.class,
                        ex -> new ResponseStatusException(
                                HttpStatus.BAD_GATEWAY,
                                "Bank rates provider unavailable",
                                ex));
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    @GetMapping("/policy-rate/latest")
    public PolicyRateLatestDto policyRateLatest() {
        return policyRateHistoryService.loadLatest();
    }

    @GetMapping("/policy-rate/history")
    public PolicyRateHistoryResponseDto policyRateHistory(
            @RequestParam(defaultValue = "5Y") String range,
            @RequestParam(defaultValue = "WEEKLY") String frequency
    ) {
        if (!"5Y".equalsIgnoreCase(range == null ? "" : range.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported range (use range=5Y)");
        }
        if (!"WEEKLY".equalsIgnoreCase(frequency == null ? "" : frequency.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported frequency (use frequency=WEEKLY)");
        }
        return policyRateHistoryService.loadFiveYearWeeklyFromDb();
    }

    @GetMapping("/repo/latest")
    public RepoRateLatestDto repoRateLatest() {
        return repoRateHistoryService.loadLatest();
    }

    @GetMapping("/repo/history")
    public PolicyRateHistoryResponseDto repoRateHistory(
            @RequestParam(defaultValue = "5Y") String range
    ) {
        if (!"5Y".equalsIgnoreCase(range == null ? "" : range.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported range (use range=5Y)");
        }
        return repoRateHistoryService.loadFiveYearWeeklyFromDb();
    }

    @GetMapping("/tl-deposit/latest")
    public TlDepositLatestDto tlDepositLatest(@RequestParam(required = false) String maturity) {
        try {
            return tlDepositHistoryService.loadLatest(maturity);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/cpi/latest")
    public CpiLatestDto cpiLatest(@RequestParam(defaultValue = "YEARLY_PCT") String metric) {
        try {
            return cpiHistoryService.loadLatest(CpiMetric.parse(metric));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/cpi/history")
    public PolicyRateHistoryResponseDto cpiHistory(
            @RequestParam(defaultValue = "YEARLY_PCT") String metric,
            @RequestParam(defaultValue = "5Y") String range
    ) {
        if (!"5Y".equalsIgnoreCase(range == null ? "" : range.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported range (use range=5Y)");
        }
        try {
            return cpiHistoryService.loadFiveYearMonthlyFromDb(CpiMetric.parse(metric));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/tl-deposit/history")
    public PolicyRateHistoryResponseDto tlDepositHistory(
            @RequestParam(defaultValue = "5Y") String range,
            @RequestParam(defaultValue = "WEEKLY") String frequency,
            @RequestParam(required = false) String maturity
    ) {
        if (!"5Y".equalsIgnoreCase(range == null ? "" : range.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported range (use range=5Y)");
        }
        if (!"WEEKLY".equalsIgnoreCase(frequency == null ? "" : frequency.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported frequency (use frequency=WEEKLY)");
        }
        try {
            return tlDepositHistoryService.loadFiveYearWeeklyFromDb(maturity);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/tl-deposit/index/latest")
    public TlDepositIndexLatestDto tlDepositIndexLatest(
            @RequestParam(required = false) String maturity,
            @RequestParam(required = false) LocalDate asOf
    ) {
        try {
            return tlDepositIndexService.loadLatest(maturity, asOf);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/tl-deposit/index/history")
    public TlDepositIndexHistoryResponseDto tlDepositIndexHistory(
            @RequestParam(required = false) String maturity,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        try {
            return tlDepositIndexService.loadHistory(maturity, from, to);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}
