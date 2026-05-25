package com.company.finance_api.watchlist.application;

import com.company.finance_api.watchlist.infrastructure.http.dto.WatchlistItemDto;
import java.util.List;

/** WatchlistService iş mantığını uygular (watchlist service). */
public interface WatchlistService {

  /** addToWatchlist sözleşmesi. */
  void addToWatchlist(Long instrumentId);

  /** removeFromWatchlist sözleşmesi. */
  void removeFromWatchlist(Long instrumentId);

  /** getMyWatchlist sözleşmesi. */
  List<WatchlistItemDto> getMyWatchlist();
}
