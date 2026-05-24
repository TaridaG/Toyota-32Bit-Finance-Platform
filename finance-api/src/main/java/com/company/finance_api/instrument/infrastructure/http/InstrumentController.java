package com.company.finance_api.instrument.infrastructure.http;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.dto.InstrumentResponse;
import com.company.finance_api.service.InstrumentService;
import com.company.finance_api.shared.web.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Enstrüman arama ve detay REST endpoint'lerini sunar. */
@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

  private final InstrumentService instrumentService;

  public InstrumentController(InstrumentService instrumentService) {
    this.instrumentService = instrumentService;
  }

  /** HTTP handler — Tekil kayıt veya koleksiyon döner. */
  @GetMapping
  public ApiResponse<List<InstrumentResponse>> getAllInstruments() {

    List<InstrumentResponse> response =
        instrumentService.getAllActive().stream().map(this::toResponse).toList();

    return ApiResponse.success(response);
  }

  private InstrumentResponse toResponse(Instrument instrument) {
    return new InstrumentResponse(
        instrument.getId(),
        instrument.getSymbol(),
        instrument.getName(),
        instrument.getType().name(),
        instrument.getExchange().name());
  }
}
