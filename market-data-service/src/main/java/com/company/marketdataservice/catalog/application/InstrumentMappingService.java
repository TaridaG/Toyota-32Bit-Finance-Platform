package com.company.marketdataservice.catalog.application;
import com.company.marketdataservice.catalog.infrastructure.persistence.InstrumentCatalogEntry;
import com.company.marketdataservice.catalog.infrastructure.persistence.InstrumentCatalogRepository;
import com.company.marketdataservice.catalog.infrastructure.persistence.ProviderInstrumentMapping;
import com.company.marketdataservice.catalog.infrastructure.persistence.ProviderInstrumentMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Provider sembollerini katalog enstrüman kimliğine çözen mapping servisi.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstrumentMappingService {

    private final ProviderInstrumentMappingRepository mappingRepository;
    private final InstrumentCatalogRepository catalogRepository;

    /**
     * Sembol veya mapping çözümlemesi yapar.
         * @param provider provider adı
         * @param symbol enstrüman sembolü
         * @return işlem sonucu
         */
    public Optional<Long> resolveInstrument(String provider, String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return Optional.empty();
        }
        String normalized = provider == null ? "" : provider.trim();
        if (normalized.isEmpty() || "COMPOSITE".equalsIgnoreCase(normalized)) {
            return resolveComposite(symbol);
        }
        if ("COMPOSITE_FX".equalsIgnoreCase(normalized)) {
            return resolveComposite(symbol);
        }
        Optional<Long> resolved = mappingRepository
                .findFirstByProviderIgnoreCaseAndProviderSymbolAndActiveTrueOrderByPriorityAsc(
                        normalized,
                        symbol
                )
                .flatMap(this::toActiveCatalogInstrumentId);
        if (resolved.isEmpty() && "YAHOO".equalsIgnoreCase(normalized) && !symbol.contains(".")) {
            resolved = mappingRepository
                    .findFirstByProviderIgnoreCaseAndProviderSymbolAndActiveTrueOrderByPriorityAsc(
                            normalized,
                            symbol + ".IS"
                    )
                    .flatMap(this::toActiveCatalogInstrumentId);
        }
        if (resolved.isPresent()) {
            return resolved;
        }
        if ("EXCHANGE_RATE_API".equalsIgnoreCase(normalized) || "EXCHANGE_API".equalsIgnoreCase(normalized)) {
            return mappingRepository
                    .findFirstByProviderIgnoreCaseAndProviderSymbolAndActiveTrueOrderByPriorityAsc(
                            "TCMB",
                            symbol
                    )
                    .flatMap(this::toActiveCatalogInstrumentId);
        }
        return Optional.empty();
    }

    /**
     * Verilen enstrüman kimliği için aktif provider mapping kayıtlarını öncelik sırasıyla döner.
     *
     * @param instrumentId katalog enstrüman kimliği
     * @return aktif mapping listesi; kimlik {@code null} ise boş liste
     */
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
