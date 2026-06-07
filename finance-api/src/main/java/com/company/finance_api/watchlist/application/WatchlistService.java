package com.company.finance_api.watchlist.application;

import com.company.finance_api.watchlist.infrastructure.http.dto.WatchlistItemDto;
import java.util.List;

/** WatchlistService iş mantığını uygular (watchlist service). */
public interface WatchlistService {

  /** Enstrümanı kullanıcının watchlist'ine ekler. */
  void addToWatchlist(Long instrumentId);

  /** Enstrümanı watchlist'ten kaldırır. */
  void removeFromWatchlist(Long instrumentId);

  /** Oturum açmış kullanıcının watchlist öğelerini döner. */
  List<WatchlistItemDto> getMyWatchlist();
}
