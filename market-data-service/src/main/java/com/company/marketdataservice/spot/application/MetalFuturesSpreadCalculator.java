package com.company.marketdataservice.spot.application;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Yahoo metal futures USD fiyatı ile TRY spot mid arasındaki spread'i hesaplar.
 */
public final class MetalFuturesSpreadCalculator {

    private MetalFuturesSpreadCalculator() {}

    /**
     * İş mantığı operasyonunu çalıştırır.
         * @param futuresUsd girdi parametresi
         * @param spotTryMid girdi parametresi
         * @param impliedTryFromFutures girdi parametresi
         * @param spreadAbsTry girdi parametresi
         * @param spreadPct girdi parametresi
         */
    public record SpreadResult(
            BigDecimal futuresUsd,
            BigDecimal spotTryMid,
            BigDecimal impliedTryFromFutures,
            BigDecimal spreadAbsTry,
            BigDecimal spreadPct) {
    }

    /**
     * Yahoo metal futures USD/oz fiyatını TRY spot mid ile karşılaştırır (ör. {@code GC=F} vs {@code XAUTRY}).
     *
     * @param futuresUsd futures USD/oz fiyatı
     * @param usdTryMid USDTRY mid kuru
     * @param spotTryMid TRY spot mid fiyatı
     * @return spread sonucu; geçersiz girdide {@code null}
     */
    public static SpreadResult compute(BigDecimal futuresUsd, BigDecimal usdTryMid, BigDecimal spotTryMid) {
        if (futuresUsd == null
                || usdTryMid == null
                || spotTryMid == null
                || futuresUsd.compareTo(BigDecimal.ZERO) <= 0
                || usdTryMid.compareTo(BigDecimal.ZERO) <= 0
                || spotTryMid.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal impliedTry = futuresUsd.multiply(usdTryMid).setScale(4, RoundingMode.HALF_UP);
        BigDecimal spreadAbs = impliedTry.subtract(spotTryMid).setScale(4, RoundingMode.HALF_UP);
        BigDecimal spreadPct = spreadAbs
                .divide(spotTryMid, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        return new SpreadResult(futuresUsd, spotTryMid, impliedTry, spreadAbs, spreadPct);
    }
}
