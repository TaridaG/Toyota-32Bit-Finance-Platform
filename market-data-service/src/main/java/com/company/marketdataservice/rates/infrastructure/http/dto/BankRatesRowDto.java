package com.company.marketdataservice.rates.infrastructure.http.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/**
 * `makro oran` REST API için HTTP DTO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BankRatesRowDto {

    private String slug;
    private String name;
    private String logoUrl;
    private String detailUrl;
    private BigDecimal buy;
    private BigDecimal sell;
    private BigDecimal spread;
    private BigDecimal spreadPercent;
    private boolean bestBuy;
    private boolean bestSell;
    private boolean bestSpreadMin;
    private boolean bestSpreadMax;
    private boolean bestSpreadPctMin;
    private boolean bestSpreadPctMax;

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getDetailUrl() {
        return detailUrl;
    }

    public void setDetailUrl(String detailUrl) {
        this.detailUrl = detailUrl;
    }

    public BigDecimal getBuy() {
        return buy;
    }

    public void setBuy(BigDecimal buy) {
        this.buy = buy;
    }

    public BigDecimal getSell() {
        return sell;
    }

    public void setSell(BigDecimal sell) {
        this.sell = sell;
    }

    public BigDecimal getSpread() {
        return spread;
    }

    public void setSpread(BigDecimal spread) {
        this.spread = spread;
    }

    public BigDecimal getSpreadPercent() {
        return spreadPercent;
    }

    public void setSpreadPercent(BigDecimal spreadPercent) {
        this.spreadPercent = spreadPercent;
    }

    public boolean isBestBuy() {
        return bestBuy;
    }

    public void setBestBuy(boolean bestBuy) {
        this.bestBuy = bestBuy;
    }

    public boolean isBestSell() {
        return bestSell;
    }

    public void setBestSell(boolean bestSell) {
        this.bestSell = bestSell;
    }

    public boolean isBestSpreadMin() {
        return bestSpreadMin;
    }

    public void setBestSpreadMin(boolean bestSpreadMin) {
        this.bestSpreadMin = bestSpreadMin;
    }

    public boolean isBestSpreadMax() {
        return bestSpreadMax;
    }

    public void setBestSpreadMax(boolean bestSpreadMax) {
        this.bestSpreadMax = bestSpreadMax;
    }

    public boolean isBestSpreadPctMin() {
        return bestSpreadPctMin;
    }

    public void setBestSpreadPctMin(boolean bestSpreadPctMin) {
        this.bestSpreadPctMin = bestSpreadPctMin;
    }

    public boolean isBestSpreadPctMax() {
        return bestSpreadPctMax;
    }

    public void setBestSpreadPctMax(boolean bestSpreadPctMax) {
        this.bestSpreadPctMax = bestSpreadPctMax;
    }
}
