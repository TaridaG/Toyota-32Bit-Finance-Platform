package com.company.finance_api.service;

import com.company.finance_api.dto.NewsFavoriteItemDto;

import java.util.List;

public interface NewsFavoriteService {

    void addFavorite(Long newsId);

    void removeFavorite(Long newsId);

    List<NewsFavoriteItemDto> getMyFavorites();
}
