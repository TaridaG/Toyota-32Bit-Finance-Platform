package com.company.marketdataservice.spot.application;
import com.company.marketdataservice.bootstrap.config.MetalFuturesSymbols;
import com.company.marketdataservice.spot.infrastructure.http.dto.FxRateDto;
import com.company.marketdataservice.spot.infrastructure.http.dto.MarketPriceDto;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import com.company.marketdataservice.spot.infrastructure.provider.yahoo.YahooFuturesContractMetaService;
import com.company.marketdataservice.spot.infrastructure.provider.yahoo.YahooFuturesContractMetaService.ContractMeta;
import com.company.marketdataservice.spot.infrastructure.snapshot.MarketSnapshotStore;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Metal futures kontrat meta verisini spot read model'ine zenginleştirme olarak ekler.
 */
@Component
public class MetalFuturesMarketEnricher {

    private final MarketSnapshotStore snapshotStore;
    private final MarketPriceHistoryRepository marketPriceHistoryRepository;
    private final YahooFuturesContractMetaService contractMetaService;

    public MetalFuturesMarketEnricher(
            MarketSnapshotStore snapshotStore,
            MarketPriceHistoryRepository marketPriceHistoryRepository,
            YahooFuturesContractMetaService contractMetaService) {
        this.snapshotStore = snapshotStore;
        this.marketPriceHistoryRepository = marketPriceHistoryRepository;
        this.contractMetaService = contractMetaService;
    }

    /**
     * Read model'i zenginleştirir.
         * @param prices girdi parametresi
         */
    public List<MarketPriceDto> enrich(List<MarketPriceDto> prices) {
        if (prices == null || prices.isEmpty()) {
            return prices;
        }
        BigDecimal usdTry = resolveUsdTryMid();
        Map<String, BigDecimal> spotTryBySymbol = resolveSpotTryMids();
        return prices.stream().map(p -> enrichOne(p, usdTry, spotTryBySymbol)).toList();
    }

    private MarketPriceDto enrichOne(MarketPriceDto base, BigDecimal usdTry, Map<String, BigDecimal> spotTryBySymbol) {
        if (base == null || !MetalFuturesSymbols.isFutures(base.symbol())) {
            return base;
        }
        String symbol = base.symbol().trim().toUpperCase(Locale.ROOT);
        String linkedSpot = MetalFuturesSymbols.linkedSpotTry(symbol);
        ContractMeta meta = contractMetaService.resolve(symbol);
        BigDecimal dayOpen = base.dayOpen() != null ? base.dayOpen() : latestTypedPrice(symbol, "OPEN");
        BigDecimal dayHigh = base.dayHigh() != null ? base.dayHigh() : latestTypedPrice(symbol, "HIGH");
        BigDecimal dayLow = base.dayLow() != null ? base.dayLow() : latestTypedPrice(symbol, "LOW");
        MetalFuturesSpreadCalculator.SpreadResult spread = null;
        if (usdTry != null && linkedSpot != null && base.price() != null) {
            BigDecimal spotMid = spotTryBySymbol.get(linkedSpot);
            spread = MetalFuturesSpreadCalculator.compute(base.price(), usdTry, spotMid);
        }
        return new MarketPriceDto(
                base.symbol(),
                base.price(),
                base.source(),
                base.timestamp(),
                base.volume24h(),
                base.openInterest(),
                dayOpen,
                dayHigh,
                dayLow,
                firstNonBlank(base.exchangeName(), meta.exchangeName()),
                firstNonBlank(base.underlyingSymbol(), meta.underlyingSymbol()),
                base.contractExpiry() != null ? base.contractExpiry() : meta.contractExpiry(),
                linkedSpot,
                spread != null ? spread.spreadPct() : null,
                spread != null ? spread.spreadAbsTry() : null);
    }

    private BigDecimal latestTypedPrice(String symbol, String priceType) {
        List<BigDecimal> rows = marketPriceHistoryRepository.findLatestPricesBefore(
                symbol,
                "YAHOO",
                priceType,
                java.time.Instant.now().plusSeconds(60),
                PageRequest.of(0, 1));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private BigDecimal resolveUsdTryMid() {
        for (FxRateDto fx : snapshotStore.listFx()) {
            if (fx != null && "USDTRY".equalsIgnoreCase(fx.symbol()) && fx.mid() != null) {
                return fx.mid();
            }
        }
        return null;
    }

    private Map<String, BigDecimal> resolveSpotTryMids() {
        Map<String, BigDecimal> out = new HashMap<>();
        for (FxRateDto fx : snapshotStore.listFx()) {
            if (fx == null || fx.symbol() == null || fx.mid() == null) {
                continue;
            }
            String sym = fx.symbol().trim().toUpperCase(Locale.ROOT);
            if (MetalFuturesSymbols.FUTURES_TO_SPOT_TRY.containsValue(sym)) {
                out.put(sym, fx.mid());
            }
        }
        return out;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}
