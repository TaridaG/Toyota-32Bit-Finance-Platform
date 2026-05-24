package com.company.marketdataservice.rates.infrastructure.http.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * `makro oran` REST API için HTTP DTO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BankRatesResponseDto {

    private String asset;
    private String title;
    private String sourceUrl;
    private String sourceProvider = "DOVIZ_COM";
    private Instant fetchedAt;
    private List<BankRatesRowDto> rows;

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceProvider() {
        return sourceProvider;
    }

    public void setSourceProvider(String sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public List<BankRatesRowDto> getRows() {
        return rows;
    }

    public void setRows(List<BankRatesRowDto> rows) {
        this.rows = rows;
    }
}
