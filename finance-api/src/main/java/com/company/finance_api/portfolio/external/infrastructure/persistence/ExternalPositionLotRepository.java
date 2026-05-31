package com.company.finance_api.portfolio.external.infrastructure.persistence;

import com.company.finance_api.portfolio.external.domain.ExternalPositionLot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** {@link ExternalPositionLot} kayıtları için Spring Data JPA repository. */
public interface ExternalPositionLotRepository extends JpaRepository<ExternalPositionLot, Long> {

  /** Silinmemiş lot kayıtlarının toplam sayısını döner. */
  long countByDeletedFalse();

  /** Portfolio'daki silinmemiş lot'ları edinim zamanına göre sıralı listeler. */
  List<ExternalPositionLot> findAllByPortfolioIdAndDeletedFalseOrderByAcquiredAtAsc(
      Long portfolioId);

  /** Portfolio kapsamında silinmemiş tek bir lot kaydını arar. */
  Optional<ExternalPositionLot> findByIdAndPortfolioIdAndDeletedFalse(Long id, Long portfolioId);

  /** Belirtilen portfolio'ya ait tüm lot kayıtlarını kalıcı olarak siler. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value = "DELETE FROM external_position_lots WHERE portfolio_id = :portfolioId",
      nativeQuery = true)
  void deleteAllByPortfolioId(@Param("portfolioId") Long portfolioId);
}
