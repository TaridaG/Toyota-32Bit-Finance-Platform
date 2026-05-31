package com.company.marketdataservice.bond.application;

import com.company.marketdataservice.bootstrap.config.TcmbBondMarketProperties;
import com.company.marketdataservice.catalog.application.InstrumentMappingService;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import com.company.marketdataservice.history.infrastructure.write.MarketHistoryWriteService;
import com.company.marketdataservice.shared.provider.tcmb.TcmbBondEvdsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BondHistoryBackfillServiceTest {

    @Mock
    private TcmbBondMarketProperties bondProperties;
    @Mock
    private TcmbBondEvdsClient tcmbBondEvdsClient;
    @Mock
    private InstrumentMappingService instrumentMappingService;
    @Mock
    private MarketHistoryWriteService marketHistoryWriteService;
    @Mock
    private MarketPriceHistoryRepository marketPriceHistoryRepository;

    private BondHistoryBackfillService service;

    @BeforeEach
    void setUp() {
        service = new BondHistoryBackfillService(
                bondProperties,
                tcmbBondEvdsClient,
                instrumentMappingService,
                marketHistoryWriteService,
                marketPriceHistoryRepository
        );
    }

    @Test
    void refreshTrailingWindow_whenDisabled_skipsEvdsFetch() {
        TcmbBondMarketProperties.BondHistoryRefresh refresh = new TcmbBondMarketProperties.BondHistoryRefresh();
        refresh.setEnabled(false);
        when(bondProperties.getHistoryRefresh()).thenReturn(refresh);

        service.refreshTrailingWindow();

        verifyNoInteractions(tcmbBondEvdsClient, marketHistoryWriteService);
    }
}
