package com.company.newsservice.repository;

import com.company.newsservice.domain.NewsArticle;
import com.company.newsservice.domain.enums.NewsCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    Optional<NewsArticle> findByArticleUrl(String articleUrl);

    Optional<NewsArticle> findByIdAndActiveTrue(Long id);

    @Query("""
        select n
        from NewsArticle n
        where n.active = true
          and (:category is null or n.category = :category)
        order by n.publishedAt desc
    """)
    Page<NewsArticle> searchByCategory(
            @Param("category") NewsCategory category,
            Pageable pageable
    );

    @Query("""
        select n
        from NewsArticle n
        where n.active = true
          and n.category = :category
          and (
                lower(n.title) like lower(concat('%', :q, '%'))
                or lower(coalesce(n.summary, '')) like lower(concat('%', :q, '%'))
              )
        order by n.publishedAt desc
    """)
    Page<NewsArticle> searchByCategoryAndQuery(
            @Param("category") NewsCategory category,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
        select n
        from NewsArticle n
        where n.active = true
          and (
                lower(n.title) like lower(concat('%', :q, '%'))
                or lower(coalesce(n.summary, '')) like lower(concat('%', :q, '%'))
              )
        order by n.publishedAt desc
    """)
    Page<NewsArticle> searchByQuery(
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
        select n
        from NewsArticle n
        where n.active = true
          and not exists (
              select 1
              from NewsArticleTranslation t
              where t.newsArticle.id = n.id
                and t.languageCode = :language
          )
        order by n.publishedAt desc
    """)
    Page<NewsArticle> findActiveWithoutTranslation(
            @Param("language") String language,
            Pageable pageable
    );
}