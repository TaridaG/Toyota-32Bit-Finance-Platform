package com.company.marketdataservice.catalog.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * `enstrüman kataloğu` verisi için Spring Data JPA repository.
 */
public interface ProviderInstrumentMappingRepository extends JpaRepository<ProviderInstrumentMapping, Long> {

    List<ProviderInstrumentMapping> findByProviderSymbolAndActiveTrueOrderByPriorityAsc(String providerSymbol);

    Optional<ProviderInstrumentMapping> findFirstByProviderIgnoreCaseAndProviderSymbolAndActiveTrueOrderByPriorityAsc(
            String provider,
            String providerSymbol
    );

    Optional<ProviderInstrumentMapping> findFirstByProviderIgnoreCaseAndInstrumentIdAndActiveTrueOrderByPriorityAsc(
            String provider,
            Long instrumentId
    );

    List<ProviderInstrumentMapping> findByInstrumentIdAndActiveTrueOrderByPriorityAsc(Long instrumentId);
}
