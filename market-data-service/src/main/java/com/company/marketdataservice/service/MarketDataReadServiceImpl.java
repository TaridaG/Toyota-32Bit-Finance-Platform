package com.company.marketdataservice.service;

import com.company.marketdataservice.dto.FundDto;
import com.company.marketdataservice.dto.FxRateDto;
import com.company.marketdataservice.dto.MarketPriceDto;
import com.company.marketdataservice.snapshot.MarketSnapshotStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketDataReadServiceImpl implements MarketDataReadService {

    private final MarketSnapshotStore snapshotStore;

    @Override
    public List<MarketPriceDto> getLatestPrices() {
        return snapshotStore.listPrices();
    }

    @Override
    public List<FxRateDto> getFxRates() {
        return snapshotStore.listFx();
    }

    @Override
    public List<FundDto> getFunds() {
        return snapshotStore.listFunds();
    }
}
