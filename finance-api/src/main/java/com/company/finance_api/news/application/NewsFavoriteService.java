package com.company.finance_api.news.application;

import com.company.finance_api.news.infrastructure.http.dto.NewsFavoriteItemDto;
import java.util.List;

/** NewsFavoriteService iş mantığını uygular (news favorite service). */
public interface NewsFavoriteService {

  /** Haberi kullanıcının favorilerine ekler. */
  void addFavorite(Long newsId);

  void removeFavorite(Long newsId);

  /** Oturum açmış kullanıcının favori haber listesini döner. */
  List<NewsFavoriteItemDto> getMyFavorites();
}
