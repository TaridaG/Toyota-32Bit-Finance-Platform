package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.ChartDrawingSaveDetailDto;
import com.company.finance_api.dto.ChartDrawingSavePageResponse;
import com.company.finance_api.dto.ChartDrawingSaveSummaryDto;
import com.company.finance_api.dto.CreateChartDrawingSaveRequest;
import com.company.finance_api.service.ChartDrawingSaveService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chart-drawings")
public class ChartDrawingSaveController {

    private final ChartDrawingSaveService chartDrawingSaveService;

    public ChartDrawingSaveController(ChartDrawingSaveService chartDrawingSaveService) {
        this.chartDrawingSaveService = chartDrawingSaveService;
    }

    @PostMapping
    public ApiResponse<ChartDrawingSaveDetailDto> create(@Valid @RequestBody CreateChartDrawingSaveRequest request) {
        return ApiResponse.success(chartDrawingSaveService.create(request));
    }

    @GetMapping
    public ApiResponse<List<ChartDrawingSaveSummaryDto>> listForAsset(@RequestParam String assetKey) {
        return ApiResponse.success(chartDrawingSaveService.listForAsset(assetKey));
    }

    @GetMapping("/mine")
    public ApiResponse<ChartDrawingSavePageResponse> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(chartDrawingSaveService.listPage(page, size));
    }

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
