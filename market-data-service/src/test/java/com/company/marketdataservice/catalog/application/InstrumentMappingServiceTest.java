package com.company.marketdataservice.catalog.application;

import com.company.marketdataservice.catalog.infrastructure.persistence.InstrumentCatalogEntry;
import com.company.marketdataservice.catalog.infrastructure.persistence.InstrumentCatalogRepository;
import com.company.marketdataservice.catalog.infrastructure.persistence.ProviderInstrumentMapping;
import com.company.marketdataservice.catalog.infrastructure.persistence.ProviderInstrumentMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstrumentMappingServiceTest {

    @Mock
    private ProviderInstrumentMappingRepository mappingRepository;

    @Mock
    private InstrumentCatalogRepository catalogRepository;

    @InjectMocks
    private InstrumentMappingService service;

    @Test
    void resolveInstrument_returnsEmptyForBlankSymbol() {
        assertTrue(service.resolveInstrument("FINNHUB", "  ").isEmpty());
    }

    @Test
    void resolveInstrument_mapsProviderSymbolToActiveCatalogId() {
        ProviderInstrumentMapping mapping = activeMapping("FINNHUB", "AAPL", 42L);
        InstrumentCatalogEntry catalog = activeCatalog(42L);

        when(mappingRepository.findFirstByProviderIgnoreCaseAndProviderSymbolAndActiveTrueOrderByPriorityAsc(
                eq("FINNHUB"), eq("AAPL"))).thenReturn(Optional.of(mapping));
        when(catalogRepository.findByInstrumentIdAndActiveTrue(42L)).thenReturn(Optional.of(catalog));

        assertEquals(Optional.of(42L), service.resolveInstrument("FINNHUB", "AAPL"));
    }

    @Test
    void resolveInstrument_yahooFallbackAddsIsSuffix() {
        ProviderInstrumentMapping mapping = activeMapping("YAHOO", "THYAO.IS", 7L);
        InstrumentCatalogEntry catalog = activeCatalog(7L);

        when(mappingRepository.findFirstByProviderIgnoreCaseAndProviderSymbolAndActiveTrueOrderByPriorityAsc(
                eq("YAHOO"), eq("THYAO"))).thenReturn(Optional.empty());
        when(mappingRepository.findFirstByProviderIgnoreCaseAndProviderSymbolAndActiveTrueOrderByPriorityAsc(
                eq("YAHOO"), eq("THYAO.IS"))).thenReturn(Optional.of(mapping));
        when(catalogRepository.findByInstrumentIdAndActiveTrue(7L)).thenReturn(Optional.of(catalog));

        assertEquals(Optional.of(7L), service.resolveInstrument("YAHOO", "THYAO"));
    }

    @Test
    void getMappingsForInstrument_returnsEmptyWhenIdNull() {
        assertTrue(service.getMappingsForInstrument(null).isEmpty());
    }

    @Test
    void getMappingsForInstrument_delegatesToRepository() {
        ProviderInstrumentMapping row = activeMapping("FINNHUB", "AAPL", 1L);
        when(mappingRepository.findByInstrumentIdAndActiveTrueOrderByPriorityAsc(1L)).thenReturn(List.of(row));

        assertEquals(1, service.getMappingsForInstrument(1L).size());
    }

    private static ProviderInstrumentMapping activeMapping(String provider, String symbol, long instrumentId) {
        ProviderInstrumentMapping mapping = new ProviderInstrumentMapping();
        mapping.setProvider(provider);
        mapping.setProviderSymbol(symbol);
        mapping.setInstrumentId(instrumentId);
        mapping.setActive(true);
        return mapping;
    }

    private static InstrumentCatalogEntry activeCatalog(long instrumentId) {
        InstrumentCatalogEntry entry = new InstrumentCatalogEntry();
        entry.setInstrumentId(instrumentId);
        entry.setActive(true);
        return entry;
    }
}
