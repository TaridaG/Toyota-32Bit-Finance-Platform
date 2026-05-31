package com.company.marketdataservice.history.infrastructure.http;
import com.company.marketdataservice.history.infrastructure.http.dto.IngestionStatusResponseDto;
import com.company.marketdataservice.history.infrastructure.orchestration.IngestionStatusQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * `geçmiş veri ve backfill` REST endpoint'lerini expose eden HTTP controller.
 */
@RestController
@RequestMapping("/api/v1/market/ingestion")
@RequiredArgsConstructor
public class IngestionStatusController {

    private final IngestionStatusQueryService ingestionStatusQueryService;

    @GetMapping("/status")
    public IngestionStatusResponseDto status(
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String symbolPrefix
    ) {
        return ingestionStatusQueryService.getStatus(assetType, status, symbolPrefix);
    }
}
