package com.company.marketdataservice.bootstrap.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * `uygulama bootstrap` feature yapılandırma property'leri (`application.yml` prefix).
 */
@Component
@ConfigurationProperties(prefix = "market.evds")
public class MarketEvdsProperties {

    private String apiKey = "";
    private String baseUrl = "https://evds3.tcmb.gov.tr/igmevdsms-dis";
    /**
     * EVDS series for the policy-rate card (EVDS points → weekly DB rows).
     * <p>Default {@code TP_BISPOLFAIZ_TUR} = EVDS “Merkez Bankası politika faizi” (BIS / TCMB serisi; aylık).
     * Override with {@code TP.FG.J0} if you prefer the 1-week repo level instead.</p>
     * <p>{@code TP.APIFON4} = günlük <strong>ağırlıklı ortalama fonlama maliyeti</strong> (politika faizi değildir).</p>
     */
    private String policyRateSeries = "TP_BISPOLFAIZ_TUR";

    /**
     * EVDS {@code frequency} query param when non-blank (e.g. {@code 5} = monthly for {@code TP_BISPOLFAIZ_TUR}).
     * Leave empty for daily series such as {@code TP.APIFON4}.
     */
    private String policyRateEvdsFrequency = "5";

    /**
     * EVDS series for TL deposit weighted-average rates (stock), one per maturity bucket (MT01…MT06).
     */
    private List<String> tlDepositEvdsSeries = new ArrayList<>(List.of(
            "TP.MT210AGS.TRY.MT01",
            "TP.MT210AGS.TRY.MT02",
            "TP.MT210AGS.TRY.MT03",
            "TP.MT210AGS.TRY.MT04",
            "TP.MT210AGS.TRY.MT05",
            "TP.MT210AGS.TRY.MT06"
    ));

    /** EVDS {@code frequency} for deposit series (typically {@code 5} = monthly, same as policy). */
    private String tlDepositEvdsFrequency = "5";

    /** Suffix of the series shown on the dashboard card (default {@code MT04} = up to 1 year). */
    private String tlDepositCardMaturity = "MT04";

    /** EVDS TÜFE genel endeks (2003=100), aylık {@code frequency=5}. Aylık/yıllık % değişimler bu seriden türetilir. */
    private String cpiIndexSeries = "TP.FG.J0";

    private String cpiEvdsFrequency = "5";

    /**
     * EVDS series for the 1-week repo rate card (default {@code TP_BISPOLFAIZ_TUR}, aligned with Investing.com repo indicator).
     */
    private String repoRateSeries = "TP_BISPOLFAIZ_TUR";

    /** EVDS {@code frequency} for repo series (typically {@code 5} = monthly). */
    private String repoRateEvdsFrequency = "5";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPolicyRateSeries() {
        return policyRateSeries;
    }

    public void setPolicyRateSeries(String policyRateSeries) {
        this.policyRateSeries = policyRateSeries;
    }

    public String getPolicyRateEvdsFrequency() {
        return policyRateEvdsFrequency;
    }

    public void setPolicyRateEvdsFrequency(String policyRateEvdsFrequency) {
        this.policyRateEvdsFrequency = policyRateEvdsFrequency;
    }

    public List<String> getTlDepositEvdsSeries() {
        return tlDepositEvdsSeries;
    }

    public void setTlDepositEvdsSeries(List<String> tlDepositEvdsSeries) {
        this.tlDepositEvdsSeries = tlDepositEvdsSeries != null ? tlDepositEvdsSeries : new ArrayList<>();
    }

    public String getTlDepositEvdsFrequency() {
        return tlDepositEvdsFrequency;
    }

    public void setTlDepositEvdsFrequency(String tlDepositEvdsFrequency) {
        this.tlDepositEvdsFrequency = tlDepositEvdsFrequency;
    }

    public String getTlDepositCardMaturity() {
        return tlDepositCardMaturity;
    }

    public void setTlDepositCardMaturity(String tlDepositCardMaturity) {
        this.tlDepositCardMaturity = tlDepositCardMaturity;
    }

    public String getCpiIndexSeries() {
        return cpiIndexSeries;
    }

    public void setCpiIndexSeries(String cpiIndexSeries) {
        this.cpiIndexSeries = cpiIndexSeries;
    }

    public String getCpiEvdsFrequency() {
        return cpiEvdsFrequency;
    }

    public void setCpiEvdsFrequency(String cpiEvdsFrequency) {
        this.cpiEvdsFrequency = cpiEvdsFrequency;
    }

    public String getRepoRateSeries() {
        return repoRateSeries;
    }

    public void setRepoRateSeries(String repoRateSeries) {
        this.repoRateSeries = repoRateSeries;
    }

    public String getRepoRateEvdsFrequency() {
        return repoRateEvdsFrequency;
    }

    public void setRepoRateEvdsFrequency(String repoRateEvdsFrequency) {
        this.repoRateEvdsFrequency = repoRateEvdsFrequency;
    }
}
