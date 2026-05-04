package com.company.marketdataservice.api;

import com.company.marketdataservice.dto.IngestionStatusResponseDto;
import com.company.marketdataservice.service.historical.IngestionStatusQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/ingestion")
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
