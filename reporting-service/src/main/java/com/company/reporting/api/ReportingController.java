package com.company.reporting.api;

import com.company.reporting.application.ReportGenerationService;
import com.company.reporting.application.ReportQueryService;
import com.company.reporting.common.ApiResponse;
import com.company.reporting.dto.CreateInstrumentReportRequest;
import com.company.reporting.dto.ReportContentResponse;
import com.company.reporting.dto.ReportMetadataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportGenerationService reportGenerationService;
    private final ReportQueryService reportQueryService;

    @PostMapping("/instrument")
    public ApiResponse<ReportMetadataResponse> createInstrumentReport(
            @Valid @RequestBody CreateInstrumentReportRequest request
    ) {
        return ApiResponse.success(reportGenerationService.generateInstrumentReport(request));
    }

    @GetMapping("/{reportId}")
    public ApiResponse<ReportMetadataResponse> getReport(
            @PathVariable UUID reportId
    ) {
        return ApiResponse.success(reportQueryService.getReportMetadata(reportId));
    }

    @GetMapping("/{reportId}/download")
    public ApiResponse<ReportContentResponse> download(
            @PathVariable UUID reportId
    ) {
        return ApiResponse.success(reportQueryService.download(reportId));
    }
}