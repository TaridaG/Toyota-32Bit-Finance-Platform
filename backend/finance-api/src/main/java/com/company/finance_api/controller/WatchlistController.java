package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.AddWatchlistItemRequest;
import com.company.finance_api.dto.WatchlistItemDto;
import com.company.finance_api.service.WatchlistService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public ApiResponse<List<WatchlistItemDto>> getMyWatchlist() {
        return ApiResponse.success(watchlistService.getMyWatchlist());
    }

    @PostMapping
    public ApiResponse<Void> addToWatchlist(@Valid @RequestBody AddWatchlistItemRequest request) {
        watchlistService.addToWatchlist(request.getInstrumentId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{instrumentId}")
    public ApiResponse<Void> removeFromWatchlist(@PathVariable Long instrumentId) {
        watchlistService.removeFromWatchlist(instrumentId);
        return ApiResponse.success(null);
    }
}
