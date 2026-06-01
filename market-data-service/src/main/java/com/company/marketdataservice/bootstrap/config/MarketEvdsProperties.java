package com.company.marketdataservice.bootstrap.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TCMB EVDS makro veri yapılandırması ({@code market.evds}): API anahtarı, base URL ve
 * politika faizi, TL mevduat, TÜFE ve repo kartları için seri kodları.
 */
@Component
@ConfigurationProperties(prefix = "market.evds")
public class MarketEvdsProperties {

    private String apiKey = "";
    private String baseUrl = "https://evds3.tcmb.gov.tr/igmevdsms-dis";
    /**
     * Politika faizi kartı EVDS seri kodu (noktalar → haftalık DB satırları).
     * <p>Varsayılan {@code TP_BISPOLFAIZ_TUR} = Merkez Bankası politika faizi (BIS/TCMB; aylık).
     * 1 haftalık repo seviyesi için {@code TP.FG.J0} ile override edilebilir.</p>
     * <p>{@code TP.APIFON4} günlük ağırlıklı ortalama fonlama maliyetidir; politika faizi değildir.</p>
     */
    private String policyRateSeries = "TP_BISPOLFAIZ_TUR";

    /**
     * Politika faizi serisi için EVDS {@code frequency} query parametresi (ör. {@code 5} = aylık).
     * Günlük serilerde boş bırakılır ({@code TP.APIFON4} gibi).
     */
    private String policyRateEvdsFrequency = "5";

    /**
     * TL mevduat ağırlıklı ortalama faiz serileri; her vade kovası için bir kod (MT01…MT06).
     */
    private List<String> tlDepositEvdsSeries = new ArrayList<>(List.of(
            "TP.MT210AGS.TRY.MT01",
            "TP.MT210AGS.TRY.MT02",
            "TP.MT210AGS.TRY.MT03",
            "TP.MT210AGS.TRY.MT04",
            "TP.MT210AGS.TRY.MT05",
            "TP.MT210AGS.TRY.MT06"
    ));

    /** TL mevduat serileri için EVDS {@code frequency} (genelde {@code 5} = aylık). */
    private String tlDepositEvdsFrequency = "5";

    /** Dashboard kartında gösterilen vade soneki (varsayılan {@code MT04} = 1 yıla kadar). */
    private String tlDepositCardMaturity = "MT04";

    /** EVDS TÜFE genel endeks (2003=100); aylık {@code frequency=5}. Aylık/yıllık % değişimler buradan türetilir. */
    private String cpiIndexSeries = "TP.FG.J0";

    private String cpiEvdsFrequency = "5";

    /**
     * 1 haftalık repo faizi kartı EVDS seri kodu (varsayılan {@code TP_BISPOLFAIZ_TUR}).
     */
    private String repoRateSeries = "TP_BISPOLFAIZ_TUR";

    /** Repo serisi için EVDS {@code frequency} (genelde {@code 5} = aylık). */
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
