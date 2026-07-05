package com.company.marketdataservice.viop.infrastructure.http;

import com.company.marketdataservice.viop.application.ViopMarketReadService;
import com.company.marketdataservice.viop.domain.ViopContractSegment;
import com.company.marketdataservice.viop.infrastructure.http.dto.ViopActiveContractDto;
import com.company.marketdataservice.viop.infrastructure.http.dto.ViopSettlementHistoryPointDto;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * `VIOP` REST endpoint'lerini expose eden HTTP controller.
 */
@RestController
@RequestMapping("/api/v1/market/viop")
public class ViopMarketController {

    private final ViopMarketReadService viopMarketReadService;

    public ViopMarketController(ViopMarketReadService viopMarketReadService) {
        this.viopMarketReadService = viopMarketReadService;
    }

    /**
     * Aktif VIOP sözleşmelerini ve son settlement snapshot'ını döndüren REST endpoint.
     */
    @GetMapping("/contracts/active")
    public List<ViopActiveContractDto> activeContracts(
            @RequestParam(name = "segment", required = false, defaultValue = "rates_bonds") String segment) {
        ViopContractSegment resolved =
                ViopContractSegment.fromQuery(segment)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST, "Unknown VIOP segment: " + segment));
        return viopMarketReadService.getActiveContracts(resolved);
    }

    /**
     * Belirtilen sözleşme kodu için tarih aralığındaki günlük settlement geçmişini döndüren REST endpoint.
     */
    @GetMapping("/contracts/{contractCode}/history")
    public List<ViopSettlementHistoryPointDto> contractHistory(
            @PathVariable String contractCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return viopMarketReadService.getContractHistory(contractCode, from, to);
    }
}
