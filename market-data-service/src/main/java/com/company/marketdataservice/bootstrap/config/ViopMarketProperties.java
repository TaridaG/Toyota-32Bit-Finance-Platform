package com.company.marketdataservice.bootstrap.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * VIOP ingest configuration for BIST official derivatives files.
 */
@Component
@ConfigurationProperties(prefix = "market.viop")
public class ViopMarketProperties {

    private boolean enabled = false;
    private String cron = "0 40 19 * * MON-FRI";
    private String zone = "Europe/Istanbul";
    private int connectTimeoutMs = 15000;
    private int readTimeoutMs = 25000;
    private long requestSpacingMs = 250L;
    private String baseUrl = "https://www.borsaistanbul.com";
    private String contractsPathTemplate = "/viopdata/viopms_{yyyyMMdd}.csv";
    private String settlementPathTemplate = "/viopdata/viop_{yyyyMMdd}.csv";
    private int sourceLookupDays = 10;
    private List<String> contractsUrls = new ArrayList<>();
    private List<String> settlementUrls = new ArrayList<>();
    private List<String> bondKeywords =
            new ArrayList<>(List.of("DIBS", "TAHVIL", "BONO", "TRBOND", "GOVBOND", "TRT", "KESN"));
    private List<String> rateKeywords = new ArrayList<>(List.of("TLREF", "FAIZ", "INTEREST"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public long getRequestSpacingMs() {
        return requestSpacingMs;
    }

    public void setRequestSpacingMs(long requestSpacingMs) {
        this.requestSpacingMs = requestSpacingMs;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getContractsPathTemplate() {
        return contractsPathTemplate;
    }

    public void setContractsPathTemplate(String contractsPathTemplate) {
        this.contractsPathTemplate = contractsPathTemplate;
    }

    public String getSettlementPathTemplate() {
        return settlementPathTemplate;
    }

    public void setSettlementPathTemplate(String settlementPathTemplate) {
        this.settlementPathTemplate = settlementPathTemplate;
    }

    public int getSourceLookupDays() {
        return sourceLookupDays;
    }

    public void setSourceLookupDays(int sourceLookupDays) {
        this.sourceLookupDays = sourceLookupDays;
    }

    public List<String> getContractsUrls() {
        return contractsUrls;
    }

    public void setContractsUrls(List<String> contractsUrls) {
        this.contractsUrls = normalizeUrls(contractsUrls);
    }

    public List<String> getSettlementUrls() {
        return settlementUrls;
    }

    public void setSettlementUrls(List<String> settlementUrls) {
        this.settlementUrls = normalizeUrls(settlementUrls);
    }

    public List<String> getBondKeywords() {
        return bondKeywords;
    }

    public void setBondKeywords(List<String> bondKeywords) {
        this.bondKeywords = bondKeywords == null ? new ArrayList<>() : new ArrayList<>(bondKeywords);
    }

    public List<String> getRateKeywords() {
        return rateKeywords;
    }

    public void setRateKeywords(List<String> rateKeywords) {
        this.rateKeywords = rateKeywords == null ? new ArrayList<>() : new ArrayList<>(rateKeywords);
    }

    private static List<String> normalizeUrls(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> out = new ArrayList<>();
        for (String row : raw) {
            if (row == null) {
                continue;
            }
            String trimmed = row.trim();
            if (!trimmed.isBlank()) {
                out.add(trimmed);
            }
        }
        return out;
    }
}

