package com.company.marketdataservice.instrument;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderInstrumentMappingRepository extends JpaRepository<ProviderInstrumentMapping, Long> {

    List<ProviderInstrumentMapping> findByProviderSymbolAndActiveTrueOrderByPriorityAsc(String providerSymbol);

    Optional<ProviderInstrumentMapping> findFirstByProviderIgnoreCaseAndProviderSymbolAndActiveTrueOrderByPriorityAsc(
            String provider,
            String providerSymbol
    );

    List<ProviderInstrumentMapping> findByInstrumentIdAndActiveTrueOrderByPriorityAsc(Long instrumentId);
}
