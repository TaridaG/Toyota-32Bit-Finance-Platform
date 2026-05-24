package com.company.notification.watchlist.infrastructure.persistence;

import com.company.notification.watchlist.domain.WatchlistProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bildirim fan-out için kullanılan watchlist projection read model'ine JPA erişimi.
 */
public interface WatchlistProjectionRepository extends JpaRepository<WatchlistProjection, Long> {

    /** Kullanıcı–instrument çifti için projection satırını varsa döner. */
    Optional<WatchlistProjection> findByUserIdAndInstrumentId(UUID userId, Long instrumentId);

    /** Bir instrument'ın aktif takipçilerini listeler (haber ve insight event yönlendirmesinde kullanılır). */
    List<WatchlistProjection> findByInstrumentIdAndActiveTrue(Long instrumentId);

    /** Aktif projection satırlarını sayar; Micrometer gauge olarak expose edilir. */
    long countByActiveTrue();
}
