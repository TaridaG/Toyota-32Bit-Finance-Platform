package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.InstrumentResponse;
import com.company.finance_api.domain.Instrument;
import com.company.finance_api.service.InstrumentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping
    public ApiResponse<List<InstrumentResponse>> getAllInstruments() {

        List<InstrumentResponse> response =
                instrumentService.getAllActive()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ApiResponse.success(response);
    }

    private InstrumentResponse toResponse(Instrument instrument) {
        return new InstrumentResponse(
                instrument.getId(),
                instrument.getSymbol(),
                instrument.getName(),
                instrument.getType().name(),
                instrument.getExchange().name()
        );
    }
}
