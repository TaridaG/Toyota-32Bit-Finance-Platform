package com.company.marketdataservice.fundamentals.infrastructure.provider;
import com.company.marketdataservice.fundamentals.infrastructure.http.dto.InstrumentFundamentalsDto;
import com.company.marketdataservice.fundamentals.infrastructure.persistence.InstrumentSharesOutstandingRepository;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import com.company.marketdataservice.catalog.infrastructure.persistence.InstrumentCatalogEntry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * `temel veri (fundamentals)` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
@Component
public class InternalMetadataFundamentalsProvider implements InstrumentFundamentalsProvider {
    private static final String PROVIDER = "INTERNAL_META";
    private final InstrumentSharesOutstandingRepository sharesRepository;
    private final MarketPriceHistoryRepository marketPriceHistoryRepository;

    public InternalMetadataFundamentalsProvider(
            InstrumentSharesOutstandingRepository sharesRepository,
            MarketPriceHistoryRepository marketPriceHistoryRepository
    ) {
        this.sharesRepository = sharesRepository;
        this.marketPriceHistoryRepository = marketPriceHistoryRepository;
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    @Override
    public String providerCode() {
        return PROVIDER;
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         * @param instrument girdi parametresi
         */
    @Override
    public boolean supports(InstrumentCatalogEntry instrument) {
        return true;
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @param instrument girdi parametresi
         * @param providerSymbol girdi parametresi
         * @return işlem sonucu
         */
    @Override
    public InstrumentFundamentalsDto fetch(InstrumentCatalogEntry instrument, String providerSymbol) {
        String asset = (instrument.getAssetClass() == null ? "" : instrument.getAssetClass()).toUpperCase(Locale.ROOT);
        String title;
        String industry;
        BigDecimal marketCap = null;
        BigDecimal sharesOutstanding = null;
        switch (asset) {
            case "FX" -> {
                title = instrument.getCanonicalSymbol() + " Exchange Rate";
                industry = "Foreign Exchange";
            }
            case "FUND" -> {
                title = instrument.getCanonicalSymbol() + " Fund";
                industry = "Fund";
            }
            case "BOND" -> {
                title = instrument.getCanonicalSymbol() + " Bond";
                industry = "Government Bond";
            }
            case "CRYPTO" -> {
                title = instrument.getCanonicalSymbol() + " Crypto Asset";
                industry = "Digital Asset";
            }
            default -> {
                title = instrument.getCanonicalSymbol();
                industry = asset.isBlank() ? null : asset;
            }
        }
        if ("STOCK".equals(asset)) {
            sharesOutstanding = sharesRepository.findByCanonicalSymbol(instrument.getCanonicalSymbol())
                    .map(row -> row.getSharesOutstanding())
                    .orElse(null);
            BigDecimal latestPrice = marketPriceHistoryRepository.findLatestPriceValue(instrument.getCanonicalSymbol())
                    .orElse(null);
            if (sharesOutstanding != null && latestPrice != null) {
                marketCap = latestPrice.multiply(sharesOutstanding);
            }
        }
        String currency = instrument.getQuoteCurrency() == null ? instrument.getBaseCurrency() : instrument.getQuoteCurrency();
        return new InstrumentFundamentalsDto(
                instrument.getCanonicalSymbol(),
                PROVIDER,
                providerSymbol,
                title,
                null,
                currency,
                asset,
                null,
                industry,
                null,
                marketCap,
                sharesOutstanding,
                null,
                null,
                Instant.now(),
                false,
                List.of()
        );
    }
}

