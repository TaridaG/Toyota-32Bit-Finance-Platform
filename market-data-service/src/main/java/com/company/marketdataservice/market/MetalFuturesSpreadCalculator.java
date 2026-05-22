package com.company.marketdataservice.market;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MetalFuturesSpreadCalculator {

    private MetalFuturesSpreadCalculator() {}

    public record SpreadResult(
            BigDecimal futuresUsd,
            BigDecimal spotTryMid,
            BigDecimal impliedTryFromFutures,
            BigDecimal spreadAbsTry,
            BigDecimal spreadPct) {
    }

    /**
     * Compares Yahoo futures USD/oz (e.g. {@code GC=F}) with TRY spot mid (e.g. {@code XAUTRY} = USD oz × USDTRY).
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
