package com.company.reporting.api;

import com.company.reporting.application.ReportGenerationService;
import com.company.reporting.application.ReportQueryService;
import com.company.reporting.application.ReportScheduleService;
import com.company.reporting.common.ApiResponse;
import com.company.reporting.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportGenerationService reportGenerationService;
    private final ReportQueryService reportQueryService;
    private final ReportScheduleService reportScheduleService;

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
    public ResponseEntity<ByteArrayResource> download(
            @PathVariable UUID reportId
    ) {
        ReportContentResponse response = reportQueryService.download(reportId);

        MediaType mediaType = MediaType.parseMediaType(response.getMediaType());

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(response.getFileName(), StandardCharsets.UTF_8)
                .build();

        ByteArrayResource resource = new ByteArrayResource(response.getContent());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(response.getContent().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }
    @PostMapping("/portfolio")
    public ApiResponse<ReportMetadataResponse> createPortfolioReport(
            @Valid @RequestBody CreatePortfolioReportRequest request
    ) {
        return ApiResponse.success(
                reportGenerationService.generatePortfolioReport(request)
        );
    }
    @PostMapping("/schedules/portfolio")
    public ApiResponse<ReportScheduleResponse> createPortfolioSchedule(
            @Valid @RequestBody CreatePortfolioReportScheduleRequest request
    ) {
        return ApiResponse.success(
                reportScheduleService.createPortfolioSchedule(request)
        );
    }

    @GetMapping("/schedules")
    public ApiResponse<List<ReportScheduleResponse>> listSchedules() {
        return ApiResponse.success(
                reportScheduleService.listSchedules()
        );
    }

    @PostMapping("/schedules/{scheduleId}/pause")
    public ApiResponse<ReportScheduleResponse> pauseSchedule(
            @PathVariable UUID scheduleId
    ) {
        return ApiResponse.success(
                reportScheduleService.pauseSchedule(scheduleId)
        );
    }

    @PostMapping("/schedules/{scheduleId}/activate")
    public ApiResponse<ReportScheduleResponse> activateSchedule(
            @PathVariable UUID scheduleId
    ) {
        return ApiResponse.success(
                reportScheduleService.activateSchedule(scheduleId)
        );
    }
}