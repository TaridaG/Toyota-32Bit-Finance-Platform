package com.company.finance_api.watchlist.infrastructure.http;

import com.company.finance_api.watchlist.infrastructure.http.dto.AddWatchlistItemRequest;
import com.company.finance_api.watchlist.infrastructure.http.dto.WatchlistItemDto;
import com.company.finance_api.watchlist.application.WatchlistService;
import com.company.finance_api.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Kullanıcı izleme listesi (watchlist) endpoint'leri. */
@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

  private final WatchlistService watchlistService;

  public WatchlistController(WatchlistService watchlistService) {
    this.watchlistService = watchlistService;
  }

  /** Oturum açmış kullanıcının watchlist öğelerini listeler. */
  @GetMapping
  public ApiResponse<List<WatchlistItemDto>> getMyWatchlist() {
    return ApiResponse.success(watchlistService.getMyWatchlist());
  }

  /** Enstrümanı watchlist'e ekler. */
  @PostMapping
  public ApiResponse<Void> addToWatchlist(@Valid @RequestBody AddWatchlistItemRequest request) {
    watchlistService.addToWatchlist(request.getInstrumentId());
    return ApiResponse.success(null);
  }

  /** Enstrümanı watchlist'ten çıkarır. */
  @DeleteMapping("/{instrumentId}")
  public ApiResponse<Void> removeFromWatchlist(@PathVariable Long instrumentId) {
    watchlistService.removeFromWatchlist(instrumentId);
    return ApiResponse.success(null);
  }
}
