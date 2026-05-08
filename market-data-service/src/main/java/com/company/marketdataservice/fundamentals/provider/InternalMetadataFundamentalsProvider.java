package com.company.marketdataservice.fundamentals.provider;

import com.company.marketdataservice.dto.InstrumentFundamentalsDto;
import com.company.marketdataservice.fundamentals.InstrumentSharesOutstandingRepository;
import com.company.marketdataservice.history.MarketPriceHistoryRepository;
import com.company.marketdataservice.instrument.InstrumentCatalogEntry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

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

    @Override
    public String providerCode() {
        return PROVIDER;
    }

    @Override
    public boolean supports(InstrumentCatalogEntry instrument) {
        return true;
    }

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

