package com.company.marketdataservice.service;

import com.company.marketdataservice.dto.FundDto;
import com.company.marketdataservice.dto.FxRateDto;
import com.company.marketdataservice.dto.MarketPriceDto;
import com.company.marketdataservice.history.FxRateHistoryRepository;
import com.company.marketdataservice.history.MarketPriceHistoryRepository;
import com.company.marketdataservice.snapshot.MarketSnapshotStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketDataReadServiceImpl implements MarketDataReadService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataReadServiceImpl.class);

    private final MarketSnapshotStore snapshotStore;
    private final MarketPriceHistoryRepository marketPriceHistoryRepository;
    private final FxRateHistoryRepository fxRateHistoryRepository;

    @Override
    public List<MarketPriceDto> getLatestPrices() {
        List<MarketPriceDto> live = snapshotStore.listPrices();
        if (!live.isEmpty()) {
            return live;
        }
        List<MarketPriceDto> fallback = marketPriceHistoryRepository.findLatestPricesPerSymbol()
                .stream()
                .map(row -> new MarketPriceDto(
                        row.getSymbol(),
                        row.getPrice(),
                        row.getSource(),
                        row.getTimestamp()
                ))
                .toList();
        log.warn("DB fallback result size: {}", fallback.size());
        return fallback;
    }

    @Override
    public List<FxRateDto> getFxRates() {
        List<FxRateDto> live = snapshotStore.listFx();
        if (!live.isEmpty()) {
            return live;
        }
        return fxRateHistoryRepository.findLatestRatesPerSymbol()
                .stream()
                .map(row -> new FxRateDto(
                        row.getCanonicalSymbol(),
                        row.getBid(),
                        row.getAsk(),
                        row.getMid(),
                        row.getSource(),
                        row.getObservedAt()
                ))
                .toList();
    }

    @Override
    public List<FundDto> getFunds() {
        return snapshotStore.listFunds();
    }
}
