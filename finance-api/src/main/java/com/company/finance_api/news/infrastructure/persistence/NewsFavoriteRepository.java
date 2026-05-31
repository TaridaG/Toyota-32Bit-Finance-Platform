package com.company.finance_api.news.infrastructure.persistence;

import com.company.finance_api.news.domain.NewsFavorite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** NewsFavorite entity persistence için Spring Data repository. */
public interface NewsFavoriteRepository extends JpaRepository<NewsFavorite, Long> {

  @Query(
      "select f from NewsFavorite f where f.user.id = :userId and f.active = true order by f.createdAt desc")
  List<NewsFavorite> findByUserIdAndActiveTrue(@Param("userId") UUID userId);

  @Query("select f from NewsFavorite f where f.user.id = :userId and f.newsId = :newsId")
  Optional<NewsFavorite> findByUserIdAndNewsId(
      @Param("userId") UUID userId, @Param("newsId") Long newsId);
}
