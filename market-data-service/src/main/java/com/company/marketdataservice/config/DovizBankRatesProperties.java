package com.company.marketdataservice.config;

import com.company.marketdataservice.rates.bank.BankRatesAsset;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market.bank-rates")
public class DovizBankRatesProperties {

    private long cacheTtlMs = 60_000L;
    private String userAgent = "Mozilla/5.0 (compatible; FinanceMarketDataService/1.0)";
    private Map<BankRatesAsset, String> pageUrls = defaultUrls();

    private static Map<BankRatesAsset, String> defaultUrls() {
        Map<BankRatesAsset, String> urls = new EnumMap<>(BankRatesAsset.class);
        urls.put(BankRatesAsset.USD, "https://kur.doviz.com/serbest-piyasa/amerikan-dolari");
        urls.put(BankRatesAsset.EUR, "https://kur.doviz.com/serbest-piyasa/euro");
        urls.put(BankRatesAsset.GBP, "https://kur.doviz.com/serbest-piyasa/sterlin");
        urls.put(BankRatesAsset.GOLD, "https://altin.doviz.com/gram-altin");
        return urls;
    }

    public long getCacheTtlMs() {
        return cacheTtlMs;
    }

    public void setCacheTtlMs(long cacheTtlMs) {
        this.cacheTtlMs = cacheTtlMs;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Map<BankRatesAsset, String> getPageUrls() {
        return pageUrls;
    }

    public void setPageUrls(Map<BankRatesAsset, String> pageUrls) {
        this.pageUrls = pageUrls;
    }

    public String urlFor(BankRatesAsset asset) {
        String url = pageUrls.get(asset);
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("No page URL configured for asset: " + asset);
        }
        return url;
    }
}
