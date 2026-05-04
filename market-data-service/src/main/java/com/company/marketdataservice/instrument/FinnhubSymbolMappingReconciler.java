package com.company.marketdataservice.instrument;

import com.company.marketdataservice.config.FinnhubProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Component
@Slf4j
public class FinnhubSymbolMappingReconciler {

    private static final String PROVIDER = "FINNHUB";
    private final FinnhubProperties properties;
    private final InstrumentCatalogRepository catalogRepository;
    private final ProviderInstrumentMappingRepository mappingRepository;

    public FinnhubSymbolMappingReconciler(
            FinnhubProperties properties,
            InstrumentCatalogRepository catalogRepository,
            ProviderInstrumentMappingRepository mappingRepository
    ) {
        this.properties = properties;
        this.catalogRepository = catalogRepository;
        this.mappingRepository = mappingRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reconcileMappings() {
        if (!properties.isEnabled() || properties.getSymbols() == null || properties.getSymbols().isEmpty()) {
            return;
        }
        List<String> symbols = properties.getSymbols().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .toList();
        for (String symbol : symbols) {
            InstrumentCatalogEntry instrument = catalogRepository
                    .findByCanonicalSymbolAndActiveTrue(symbol)
                    .orElse(null);
            if (instrument == null) {
                log.warn("FINNHUB_MAPPING_SKIP symbol={} reason=instrument_not_found_in_catalog", symbol);
                continue;
            }
            ProviderInstrumentMapping mapping = mappingRepository
                    .findFirstByProviderIgnoreCaseAndProviderSymbolAndActiveTrueOrderByPriorityAsc(PROVIDER, symbol)
                    .orElse(null);
            if (mapping == null) {
                mapping = new ProviderInstrumentMapping();
            }
            mapping.setProvider(PROVIDER);
            mapping.setProviderSymbol(symbol);
            mapping.setInstrumentId(instrument.getInstrumentId());
            mapping.setPriority(0);
            mapping.setActive(true);
            mappingRepository.save(mapping);
        }
        log.info("FINNHUB_MAPPING_RECONCILED symbols={}", symbols.size());
    }
}
