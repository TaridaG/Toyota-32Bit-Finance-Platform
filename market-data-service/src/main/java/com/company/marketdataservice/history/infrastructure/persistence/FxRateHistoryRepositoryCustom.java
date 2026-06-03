package com.company.marketdataservice.history.infrastructure.persistence;

import com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto;

import java.time.Instant;
import java.util.List;

/**
 * Spot-metal FX history queries (provider-priority dedup, targeted deletes for backfill refresh).
 */
public interface FxRateHistoryRepositoryCustom {

    /**
     * One mid per UTC calendar day for spot metals, preferring Minted/Yahoo-derived over TCMB gram quotes.
     */
    List<HistoryPointDto> findSpotMetalHistoryPoints(
            String canonicalSymbol,
            Instant fromInclusive,
            Instant toExclusive
    );

    int deleteBySymbolProviderAndObservedAtBetween(
            String canonicalSymbol,
            String provider,
            Instant fromInclusive,
            Instant toExclusive
    );

    /** Last two UTC daily closes with provider priority (spot metals). */
    List<FxRateHistoryRepository.DailyCloseView> findSpotMetalLastTwoDailyCloses(String canonicalSymbol);
}
