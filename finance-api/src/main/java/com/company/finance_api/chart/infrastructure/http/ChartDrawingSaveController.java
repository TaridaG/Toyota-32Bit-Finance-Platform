package com.company.finance_api.chart.infrastructure.http;

import com.company.finance_api.chart.application.ChartDrawingSaveService;
import com.company.finance_api.chart.infrastructure.http.dto.ChartDrawingSaveDetailDto;
import com.company.finance_api.chart.infrastructure.http.dto.ChartDrawingSavePageResponse;
import com.company.finance_api.chart.infrastructure.http.dto.ChartDrawingSaveSummaryDto;
import com.company.finance_api.chart.infrastructure.http.dto.CreateChartDrawingSaveRequest;
import com.company.finance_api.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Kullanıcı grafik çizim kayıtlarının CRUD endpoint'lerini sunar. */
@RestController
@RequestMapping("/api/chart-drawings")
public class ChartDrawingSaveController {

  private final ChartDrawingSaveService chartDrawingSaveService;

  public ChartDrawingSaveController(ChartDrawingSaveService chartDrawingSaveService) {
    this.chartDrawingSaveService = chartDrawingSaveService;
  }

  /** HTTP handler — Yeni kayıt oluşturur. */
  @PostMapping
  public ApiResponse<ChartDrawingSaveDetailDto> create(
      @Valid @RequestBody CreateChartDrawingSaveRequest request) {
    return ApiResponse.success(chartDrawingSaveService.create(request));
  }

  @GetMapping
  public ApiResponse<List<ChartDrawingSaveSummaryDto>> listForAsset(@RequestParam String assetKey) {
    return ApiResponse.success(chartDrawingSaveService.listForAsset(assetKey));
  }

  /** HTTP handler — Liste endpoint'i — kayıtları döner. */
  @GetMapping("/mine")
  public ApiResponse<ChartDrawingSavePageResponse> listMine(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.success(chartDrawingSaveService.listPage(page, size));
  }

  /** HTTP handler — Tekil kayıt veya koleksiyon döner. */
  @GetMapping("/{id}")
  public ApiResponse<ChartDrawingSaveDetailDto> getById(@PathVariable Long id) {
    return ApiResponse.success(chartDrawingSaveService.getById(id));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    chartDrawingSaveService.delete(id);
    return ApiResponse.success(null);
  }
}
