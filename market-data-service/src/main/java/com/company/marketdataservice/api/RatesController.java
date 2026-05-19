package com.company.marketdataservice.api;

import com.company.marketdataservice.dto.CpiLatestDto;
import com.company.marketdataservice.dto.PolicyRateHistoryResponseDto;
import com.company.marketdataservice.dto.PolicyRateLatestDto;
import com.company.marketdataservice.dto.TlDepositLatestDto;
import com.company.marketdataservice.rates.CpiHistoryService;
import com.company.marketdataservice.rates.CpiMetric;
import com.company.marketdataservice.rates.PolicyRateHistoryService;
import com.company.marketdataservice.rates.TlDepositHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rates")
public class RatesController {

    private final PolicyRateHistoryService policyRateHistoryService;
    private final TlDepositHistoryService tlDepositHistoryService;
    private final CpiHistoryService cpiHistoryService;

    public RatesController(
            PolicyRateHistoryService policyRateHistoryService,
            TlDepositHistoryService tlDepositHistoryService,
            CpiHistoryService cpiHistoryService
    ) {
        this.policyRateHistoryService = policyRateHistoryService;
        this.tlDepositHistoryService = tlDepositHistoryService;
        this.cpiHistoryService = cpiHistoryService;
    }

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
}
