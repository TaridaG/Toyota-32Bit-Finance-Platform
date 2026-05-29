package com.company.marketdataservice.history.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class BackfillChunkRepositoryImpl implements BackfillChunkRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean insertIgnore(
            String assetType,
            String symbol,
            String provider,
            Instant windowStart,
            Instant windowEnd,
            String status,
            long attemptCount,
            Instant createdAt,
            Instant updatedAt
    ) {
        int updated = entityManager.createNativeQuery("""
                        INSERT INTO mds_backfill_chunk (
                            asset_type,
                            symbol,
                            provider,
                            window_start,
                            window_end,
                            status,
                            attempt_count,
                            created_at,
                            updated_at
                        ) VALUES (
                            :assetType,
                            :symbol,
                            :provider,
                            :windowStart,
                            :windowEnd,
                            :status,
                            :attemptCount,
                            :createdAt,
                            :updatedAt
                        )
                        ON CONFLICT (asset_type, symbol, provider, window_start, window_end) DO NOTHING
                        """)
                .setParameter("assetType", assetType)
                .setParameter("symbol", symbol)
                .setParameter("provider", provider)
                .setParameter("windowStart", windowStart)
                .setParameter("windowEnd", windowEnd)
                .setParameter("status", status)
                .setParameter("attemptCount", attemptCount)
                .setParameter("createdAt", createdAt)
                .setParameter("updatedAt", updatedAt)
                .executeUpdate();
        return updated == 1;
    }
}

