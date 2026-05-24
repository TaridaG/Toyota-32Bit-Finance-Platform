package com.company.marketdataservice.history.infrastructure.orchestration;
/**
 * `geçmiş veri ve backfill` backfill/ingestion orchestration bileşeni.
 */
public final class BackfillExecutionContext {

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private BackfillExecutionContext() {
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    public static void activate() {
        ACTIVE.set(true);
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    public static void clear() {
        ACTIVE.remove();
    }

    /**
     * Durum kontrolü yapar.
         * @return işlem sonucu
         */
    public static boolean isActive() {
        return Boolean.TRUE.equals(ACTIVE.get());
    }
}
