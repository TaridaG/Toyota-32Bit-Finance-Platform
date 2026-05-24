package com.company.finance_api.portfolio.external.repository;

import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** {@link ExternalPortfolio} kayıtları için Spring Data JPA repository. */
public interface ExternalPortfolioRepository extends JpaRepository<ExternalPortfolio, Long> {

  /** Kullanıcının portfolio kayıtlarını oluşturulma zamanına göre azalan sırada listeler. */
  List<ExternalPortfolio> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

  /** Id ve kullanıcı eşleşmesiyle portfolio arar. */
  Optional<ExternalPortfolio> findByIdAndUserId(Long id, UUID userId);

  /** Kullanıcı başına portfolio sayısını döner. */
  long countByUserId(UUID userId);

  /** Belirtilen zaman aralığında oluşturulan portfolio sayısını döner (native SQL). */
  @Query(
      value =
          "SELECT COUNT(*) FROM external_portfolios WHERE created_at >= :startInclusive AND created_at < :endExclusive",
      nativeQuery = true)
  long countCreatedBetween(
      @Param("startInclusive") Timestamp startInclusive,
      @Param("endExclusive") Timestamp endExclusive);

  /** UTC günü başlamadan önce var olup o gün güncellenen portfolio sayısını döner. */
  @Query(
      value =
          """
                    SELECT COUNT(*) FROM external_portfolios
                    WHERE created_at < :dayStartUtc
                      AND updated_at >= :dayStartUtc AND updated_at < :dayEndExclusive
                    """,
      nativeQuery = true)
  long countExistingUpdatedOnUtcDay(
      @Param("dayStartUtc") Timestamp dayStartUtc,
      @Param("dayEndExclusive") Timestamp dayEndExclusive);

  /** Kullanıcı bilgisiyle birlikte portfolio kayıtlarını sayfalı listeler. */
  @Query("select p from ExternalPortfolio p join fetch p.user order by p.createdAt desc")
  List<ExternalPortfolio> findAllWithUserOrderByCreatedAtDesc(Pageable pageable);

  /** Tüm portfolio id ve kullanıcı id çiftlerini döner. */
  @Query("select p.id, p.user.id from ExternalPortfolio p")
  List<Object[]> findAllPortfolioIdAndUserId();
}
