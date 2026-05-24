package com.company.finance_api.service;

import com.company.finance_api.dto.NewsFavoriteItemDto;
import java.util.List;

/** NewsFavoriteService iş mantığını uygular (news favorite service). */
public interface NewsFavoriteService {

  /** addFavorite sözleşmesi. */
  void addFavorite(Long newsId);

  void removeFavorite(Long newsId);

  /** getMyFavorites sözleşmesi. */
  List<NewsFavoriteItemDto> getMyFavorites();
}
