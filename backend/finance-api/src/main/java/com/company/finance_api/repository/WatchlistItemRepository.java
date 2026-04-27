package com.company.finance_api.repository;

import com.company.finance_api.domain.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

    @Query("select w from WatchlistItem w where w.user.id = :userId and w.active = true")
    List<WatchlistItem> findByUserIdAndActiveTrue(@Param("userId") UUID userId);

    @Query("select w from WatchlistItem w where w.user.id = :userId and w.instrument.id = :instrumentId")
    Optional<WatchlistItem> findByUserIdAndInstrumentId(@Param("userId") UUID userId, @Param("instrumentId") Long instrumentId);

    @Query("""
            select case when count(w) > 0 then true else false end
            from WatchlistItem w
            where w.user.id = :userId and w.instrument.id = :instrumentId and w.active = true
            """)
    boolean existsByUserIdAndInstrumentIdAndActiveTrue(@Param("userId") UUID userId, @Param("instrumentId") Long instrumentId);

    @Query("select w from WatchlistItem w where w.instrument.id = :instrumentId and w.active = true")
    List<WatchlistItem> findActiveByInstrumentId(@Param("instrumentId") Long instrumentId);
}
