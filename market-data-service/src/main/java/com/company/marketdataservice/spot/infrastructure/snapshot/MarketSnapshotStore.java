package com.company.marketdataservice.spot.infrastructure.snapshot;
import com.company.marketdataservice.spot.infrastructure.http.dto.FundDto;
import com.company.marketdataservice.spot.infrastructure.http.dto.FxRateDto;
import com.company.marketdataservice.spot.infrastructure.http.dto.MarketPriceDto;
import com.company.marketdataservice.fund.domain.FundSnapshotUpdatedEvent;
import com.company.marketdataservice.fx.domain.FxSnapshotUpdatedEvent;
import com.company.marketdataservice.spot.domain.MarketPriceUpdatedEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canlı spot/fon/FX fiyatlarının in-memory snapshot deposu.
 */
@Component
public class MarketSnapshotStore {

    private final ConcurrentHashMap<String, MarketPriceDto> latestPrices = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FxRateDto> latestFx = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FundDto> latestFunds = new ConcurrentHashMap<>();

    public void recordMarketPrice(MarketPriceUpdatedEvent event) {
        if (event == null || event.instrumentSymbol() == null || event.instrumentSymbol().isBlank()) {
            return;
        }
        recordMarketPriceDto(MarketPriceDto.basic(
                event.instrumentSymbol(),
                event.price(),
                event.source(),
                event.occurredAt()));
    }

    public void recordMarketPriceDto(MarketPriceDto dto) {
        if (dto == null || dto.symbol() == null || dto.symbol().isBlank()) {
            return;
        }
        latestPrices.put(dto.symbol(), dto);
    }

    public void recordFx(FxSnapshotUpdatedEvent event) {
        if (event == null || event.canonicalSymbol() == null || event.canonicalSymbol().isBlank()) {
            return;
        }
        latestFx.put(
                event.canonicalSymbol(),
                new FxRateDto(
                        event.canonicalSymbol(),
                        event.bid(),
                        event.ask(),
                        event.mid(),
                        event.source(),
                        event.occurredAt()
                )
        );
    }

    public void recordFund(FundSnapshotUpdatedEvent event) {
        if (event == null || event.fundCode() == null || event.fundCode().isBlank()) {
            return;
        }
        latestFunds.put(
                event.fundCode(),
                new FundDto(event.fundCode(), event.nav(), event.source(), event.occurredAt())
        );
    }

    public List<MarketPriceDto> listPrices() {
        List<MarketPriceDto> out = new ArrayList<>(latestPrices.values());
        out.sort(Comparator.comparing(MarketPriceDto::symbol, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public List<FxRateDto> listFx() {
        List<FxRateDto> out = new ArrayList<>(latestFx.values());
        out.sort(Comparator.comparing(FxRateDto::symbol, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public List<FundDto> listFunds() {
        List<FundDto> out = new ArrayList<>(latestFunds.values());
        out.sort(Comparator.comparing(FundDto::fundCode, String.CASE_INSENSITIVE_ORDER));
        return out;
    }
}
