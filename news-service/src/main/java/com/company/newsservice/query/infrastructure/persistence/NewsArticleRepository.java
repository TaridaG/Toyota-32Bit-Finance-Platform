package com.company.newsservice.query.infrastructure.persistence;

import com.company.newsservice.query.domain.NewsArticle;
import com.company.newsservice.query.domain.enums.NewsCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * {@link NewsArticle} için JPA sorguları (arama, chart, translation backfill, admin metrikleri).
 */
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    /** Makale URL'sine göre kayıt döner (ingest deduplication). */
    Optional<NewsArticle> findByArticleUrl(String articleUrl);

    /** Aktif makaleyi id ile döner. */
    Optional<NewsArticle> findByIdAndActiveTrue(Long id);

    @Query("""
        select n
        from NewsArticle n
        where n.active = true
          and (:category is null or n.category = :category)
        order by n.publishedAt desc
    """)
    /** Aktif makaleleri isteğe bağlı kategori ile sayfalar. */
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
    /** Kategori ve başlık/özet metin sorgusu ile aktif makaleleri sayfalar. */
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
    /** Başlık/özet metin sorgusu ile aktif makaleleri sayfalar. */
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
    /** Belirtilen dilde çeviri satırı olmayan aktif makaleleri sayfalar (backfill için). */
    Page<NewsArticle> findActiveWithoutTranslation(
            @Param("language") String language,
            Pageable pageable
    );

    @Query("""
        select n
        from NewsArticle n
        where n.active = true
          and (n.imageUrl is null or n.imageUrl = '')
        order by n.publishedAt desc
    """)
    /** Görsel URL'si boş olan aktif makaleleri sayfalar (image backfill için). */
    Page<NewsArticle> findActiveWithoutImageUrl(Pageable pageable);

    /** Aktif makale sayısını döner. */
    long countByActiveTrue();

    /** Aktif makalelerdeki benzersiz kaynak adı sayısını döner. */
    @Query("select count(distinct n.sourceName) from NewsArticle n where n.active = true")
    long countDistinctSourceNameByActiveTrue();

    /** Verilen oluşturulma aralığındaki aktif makale sayısını döner. */
    @Query("select count(n) from NewsArticle n where n.active = true and n.createdAt >= :from and n.createdAt < :to")
    long countCreatedBetweenActiveTrue(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select count(n) from NewsArticle n
            where n.active = true
              and exists (
                  select 1 from NewsArticleTranslation t
                  where t.newsArticle = n and lower(t.languageCode) = lower(:lang)
              )
            """)
    /** Belirtilen dilde çeviri satırı olan aktif makale sayısını döner. */
    long countActiveWithTranslationLanguage(@Param("lang") String lang);

    /** Aktif makaleleri yayın tarihine göre azalan sırada sayfalar. */
    List<NewsArticle> findByActiveTrueOrderByPublishedAtDesc(Pageable pageable);

    /** Verilen yayın tarihi aralığındaki aktif makale sayısını döner. */
    @Query("select count(n) from NewsArticle n where n.active = true and n.publishedAt >= :from and n.publishedAt < :to")
    long countPublishedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select n
            from NewsArticle n
            where n.active = true
              and n.publishedAt >= :fromInclusive
              and n.publishedAt <= :toInclusive
            order by n.publishedAt asc
            """)
    /** Chart overlay için yayın tarihi aralığındaki aktif makaleleri döner. */
    List<NewsArticle> findActiveByPublishedAtBetween(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toInclusive") Instant toInclusive
    );
}
