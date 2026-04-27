package com.company.finance_api.service;

import com.company.finance_api.dto.WatchlistItemDto;

import java.util.List;

public interface WatchlistService {

    void addToWatchlist(Long instrumentId);

    void removeFromWatchlist(Long instrumentId);

    List<WatchlistItemDto> getMyWatchlist();
}
