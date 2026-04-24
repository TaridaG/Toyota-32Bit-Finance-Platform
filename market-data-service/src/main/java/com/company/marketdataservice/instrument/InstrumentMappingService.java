package com.company.marketdataservice.instrument;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstrumentMappingService {

    private final ProviderInstrumentMappingRepository mappingRepository;
    private final InstrumentCatalogRepository catalogRepository;

    public Optional<Long> resolveInstrument(String provider, String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return Optional.empty();
        }
        String normalized = provider == null ? "" : provider.trim();
        if (normalized.isEmpty() || "COMPOSITE".equalsIgnoreCase(normalized)) {
            return resolveComposite(symbol);
        }
        return mappingRepository
                .findFirstByProviderIgnoreCaseAndProviderSymbolAndActiveTrueOrderByPriorityAsc(
                        normalized,
                        symbol
                )
                .flatMap(this::toActiveCatalogInstrumentId);
    }

    public List<ProviderInstrumentMapping> getMappingsForInstrument(Long instrumentId) {
        if (instrumentId == null) {
            return List.of();
        }
        return mappingRepository.findByInstrumentIdAndActiveTrueOrderByPriorityAsc(instrumentId);
    }

    private Optional<Long> resolveComposite(String symbol) {
        List<ProviderInstrumentMapping> rows =
                mappingRepository.findByProviderSymbolAndActiveTrueOrderByPriorityAsc(symbol.trim());
        for (ProviderInstrumentMapping row : rows) {
            Optional<Long> id = toActiveCatalogInstrumentId(row);
            if (id.isPresent()) {
                return id;
            }
        }
        return Optional.empty();
    }

    private Optional<Long> toActiveCatalogInstrumentId(ProviderInstrumentMapping mapping) {
        if (mapping == null || !mapping.isActive()) {
            return Optional.empty();
        }
        return catalogRepository
                .findByInstrumentIdAndActiveTrue(mapping.getInstrumentId())
                .map(InstrumentCatalogEntry::getInstrumentId);
    }
}
